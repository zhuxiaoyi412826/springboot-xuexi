package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 令牌桶限流（Lua 脚本实现，保证「读-算-写」原子性）。
 *
 * <p>为什么不用 Java 代码算好再写？因为并发下会出现
 * 「多个线程同时读到 10 个 token，然后都认为自己能过」的问题，Lua 在 Redis 单线程里执行天然原子。
 */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

    /**
     * KEYS[1]=桶的 key；ARGV[1]=容量 ARGV[2]=每秒生成令牌数 ARGV[3]=当前毫秒 ARGV[4]=申请令牌数 ARGV[5]=key 过期秒数
     * 返回 1=放行，0=限流
     */
    private static final String TOKEN_BUCKET_LUA = """
            local key = KEYS[1]
            local capacity = tonumber(ARGV[1])
            local refill = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])
            local requested = tonumber(ARGV[4])
            local ttl = tonumber(ARGV[5])

            local bucket = redis.call('HMGET', key, 'tokens', 'ts')
            local tokens = tonumber(bucket[1])
            local ts = tonumber(bucket[2])
            if tokens == nil then
                tokens = capacity
                ts = now
            end

            local delta = math.max(0, now - ts) / 1000.0
            tokens = math.min(capacity, tokens + delta * refill)
            local allowed = 0
            if tokens >= requested then
                tokens = tokens - requested
                allowed = 1
            end
            redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
            redis.call('EXPIRE', key, ttl)
            return allowed
            """;

    private final RedisService redis;

    /** @return true=放行，false=被限流 */
    public boolean allow(String bizKey, int capacity, int refillPerSecond, int requested) {
        String key = String.format(RedisKeys.RATE_LIMIT, bizKey);
        Long r = redis.eval(TOKEN_BUCKET_LUA, Long.class, List.of(key),
                String.valueOf(capacity),
                String.valueOf(refillPerSecond),
                String.valueOf(System.currentTimeMillis()),
                String.valueOf(requested),
                "600");
        return r != null && r == 1L;
    }
}
