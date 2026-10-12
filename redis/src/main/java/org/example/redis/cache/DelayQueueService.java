package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 延迟队列（ZSet 实现）。
 *
 * <p>score = 执行时间戳，定时轮询取出 score <= now 的任务。
 * 典型场景：定时上架歌曲、30 分钟后关闭未支付订单、延迟推送通知。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelayQueueService {

    /** 原子地「取出并删除」到期任务 */
    private static final String POP_DUE_LUA = """
            local items = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1], 'LIMIT', 0, ARGV[2])
            if #items > 0 then
                for i = 1, #items do
                    redis.call('ZREM', KEYS[1], items[i])
                end
            end
            return items
            """;

    private final RedisService redis;

    /** 投递一个延迟任务 */
    public void push(String payload, long delaySeconds) {
        double score = System.currentTimeMillis() + delaySeconds * 1000.0;
        redis.zAdd(RedisKeys.DELAY_QUEUE, payload, score);
    }

    /** 取出到期任务（已删除） */
    public List<String> popDue(int limit) {
        try {
            List<String> items = redis.eval(POP_DUE_LUA, List.class,
                    List.of(RedisKeys.DELAY_QUEUE),
                    String.valueOf(System.currentTimeMillis()), String.valueOf(limit));
            return items == null ? List.of() : items;
        } catch (Exception e) {
            log.warn("取出延迟任务失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** 查看即将执行的任务（不删除） */
    public List<String> peek(int limit) {
        return new java.util.ArrayList<>(redis.zRangeByScore(RedisKeys.DELAY_QUEUE, 0, System.currentTimeMillis() + 3600_000L));
            }

    public long size() {
        return redis.zCard(RedisKeys.DELAY_QUEUE);
    }
}
