package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.entity.PlayRecord;
import org.example.redis.entity.Song;
import org.example.redis.mapper.PlayRecordMapper;
import org.example.redis.mapper.SongMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 播放事件消费者（Redis Stream）+ 播放量回写。
 *
 * <p>为什么要绕一圈？
 * 播放是极高并发的写操作。直接 UPDATE 数据库会把数据库打爆；
 * 所以：先 INCR Redis（快），再把事件写进 Stream，消费者慢慢落库（削峰填谷）。
 *
 * <p>Stream 相比 List 做队列的好处：消息可回溯、有唯一 ID、支持消费组、不会丢。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlayEventConsumer {

    private final RedisService redis;
    private final PlayRecordMapper playRecordMapper;
    private final SongMapper songMapper;

    /** 上次消费到的消息 id（生产环境应持久化，比如存 Redis） */
    private volatile String lastId = "0-0";

    @Scheduled(fixedDelay = 5000, initialDelay = 8000)
    public void consume() {
        try {
            var records = redis.xRead(RedisKeys.STREAM_PLAY, lastId, 100);
            for (var record : records) {
                Map<Object, Object> value = record.getValue();
                try {
                    PlayRecord pr = new PlayRecord();
                    pr.setSongId(Long.valueOf(String.valueOf(value.getOrDefault("songId", "0"))));
                    Object uid = value.get("userId");
                    pr.setUserId(uid == null || "null".equals(String.valueOf(uid)) ? null : Long.valueOf(String.valueOf(uid)));
                    pr.setSource("stream");
                    playRecordMapper.insert(pr);
                } catch (Exception e) {
                    log.warn("落库播放流水失败：{}", e.getMessage());
                }
                lastId = record.getId().toString();
            }
            if (!records.isEmpty()) {
                log.debug("消费播放流水 {} 条，最新 id={}", records.size(), lastId);
            }
        } catch (Exception e) {
            log.debug("读取播放流失败（可能还没有数据）：{}", e.getMessage());
        }
    }

    /** 每 30 秒把 Redis 里的播放计数同步一次到数据库 */
    @Scheduled(fixedDelay = 30_000, initialDelay = 15_000)
    public void syncPlayCount() {
        try {
            var keys = redis.scan("music:cnt:play:*", 200);
            for (String key : keys) {
                String id = key.substring(key.lastIndexOf(':') + 1);
                String val = redis.get(key);
                if (val == null) {
                    continue;
                }
                Song song = songMapper.selectById(Long.valueOf(id));
                if (song == null) {
                    continue;
                }
                long base = song.getPlayCount() == null ? 0 : song.getPlayCount();
                long redisCount = Long.parseLong(val);
                if (redisCount > base) {
                    song.setPlayCount(redisCount);
                    songMapper.updateById(song);
                }
            }
        } catch (Exception e) {
            log.warn("同步播放量失败：{}", e.getMessage());
        }
    }
}
