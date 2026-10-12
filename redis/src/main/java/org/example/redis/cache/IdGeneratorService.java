package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

/**
 * 分布式 ID 生成器（Redis INCR / INCRBY）。
 *
 * <p>INCR 是原子操作，多实例并发也不会重复；比 UUID 更短、比雪花算法更简单，
 * 适合“评论序号、流水号、短链 id”这类场景。
 */
@Service
@RequiredArgsConstructor
public class IdGeneratorService {

    private final RedisService redis;

    public long nextId(String biz) {
        Long id = redis.incr(String.format(RedisKeys.ID_GEN, biz));
        return id == null ? 0L : id;
    }

    /** 批量取一段 id（号段模式，减少 Redis 访问） */
    public long[] nextRange(String biz, int step) {
        Long end = redis.incrBy(String.format(RedisKeys.ID_GEN, biz), step);
        if (end == null) {
            return new long[0];
        }
        long start = end - step + 1;
        long[] ids = new long[step];
        for (int i = 0; i < step; i++) {
            ids[i] = start + i;
        }
        return ids;
    }
}
