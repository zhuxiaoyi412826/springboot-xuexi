package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.cache.BloomFilterService;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.entity.Favorite;
import org.example.redis.entity.Song;
import org.example.redis.mapper.FavoriteMapper;
import org.example.redis.mapper.SongMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.SongVO;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 缓存预热（应用启动时执行）。
 *
 * <p>为什么需要预热？
 * 大促/重启后如果缓存是空的，所有请求会同时打到数据库（缓存冷启动 = 雪崩）。
 * 预热就是启动时主动把热点数据灌进 Redis。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WarmUpService implements ApplicationRunner {

    private static final String BLOOM_SONG = "song";

    private final SongMapper songMapper;
    private final FavoriteMapper favoriteMapper;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final BloomFilterService bloom;

    @Override
    public void run(ApplicationArguments args) {
        try {
            warmUp();
        } catch (Exception e) {
            // 预热失败不能影响启动
            log.warn("缓存预热失败（不影响启动）：{}", e.getMessage());
        }
    }

    public void warmUp() {
        List<Song> songs = songMapper.selectList(null);
        if (songs.isEmpty()) {
            log.info("没有歌曲数据，跳过预热");
            return;
        }

        // 1) 布隆过滤器灌数据（防穿透的第一道门）
        bloom.init(BLOOM_SONG);
        for (Song s : songs) {
            bloom.add(BLOOM_SONG, String.valueOf(s.getId()));
        }
        bloom.markReady(BLOOM_SONG);
        log.info("布隆过滤器就绪，歌曲 {} 首，占用位 {}", songs.size(), bloom.bitCount(BLOOM_SONG));

        // 2) 热歌榜 ZSet：用数据库播放量初始化
        for (Song s : songs) {
            if (s.getStatus() != null && s.getStatus() == 1) {
                long score = (s.getPlayCount() == null ? 0 : s.getPlayCount());
                redis.zAdd(RedisKeys.RANK_HOT, String.valueOf(s.getId()), score);
            }
        }

        // 3) 用 pipeline 批量预热前 10 首歌曲详情（一次性网络往返）
        List<Song> hot = songs.stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .sorted((a, b) -> Long.compare(b.getPlayCount() == null ? 0 : b.getPlayCount(),
                        a.getPlayCount() == null ? 0 : a.getPlayCount()))
                .limit(10)
                .toList();
        redis.pipeline(t -> {
            for (Song s : hot) {
                cache.getHot("song", String.valueOf(s.getId()), SongVO.class, () -> SongVO.from(s));
            }
        });
        log.info("预热完成：热歌榜 {} 首，详情缓存 {} 首", songs.size(), hot.size());

        // 4) 收藏关系灌进 Set（历史数据在 Redis 里没有，第一次访问会查库，这里提前灌好）
        List<Favorite> favorites = favoriteMapper.selectList(null);
        for (Favorite f : favorites) {
            redis.sAdd(String.format(RedisKeys.FAV_SONGS, f.getUserId()), String.valueOf(f.getSongId()));
            redis.sAdd(String.format(RedisKeys.FAV_USERS, f.getSongId()), String.valueOf(f.getUserId()));
        }
        log.info("收藏关系预热 {} 条", favorites.size());
    }
}
