package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 基于 Redis 的分布式锁（知识点：SET key value NX PX + Lua 原子释放）。
 *
 * <p>三个必须满足的点：
 * <ol>
 *   <li>加锁要带唯一 value（释放时校验，防止误删别人的锁）</li>
 *   <li>解锁要用 Lua 保证“比对 + 删除”原子性</li>
 *   <li>必须设置过期时间，防止宕机后死锁</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLockService {

    /** 释放锁：value 相同才删除（原子） */
    private static final String UNLOCK_LUA =
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

    private final RedisService redis;

    /**
     * 尝试加锁（阻塞等待 waitSeconds 秒）。
     *
     * @return 锁标识（解锁时需要），获取失败返回 null
     */
    public String tryLock(String lockName, long waitSeconds, long leaseSeconds) {
        String key = String.format(RedisKeys.LOCK, lockName);
        String token = UUID.randomUUID().toString();
        long end = System.currentTimeMillis() + waitSeconds * 1000L;
        while (System.currentTimeMillis() < end) {
            if (Boolean.TRUE.equals(redis.setIfAbsent(key, token, Duration.ofSeconds(leaseSeconds)))) {
                return token;
            }
            sleepQuietly(50);
        }
        return null;
    }

    public void unlock(String lockName, String token) {
        if (token == null) {
            return;
        }
        String key = String.format(RedisKeys.LOCK, lockName);
        try {
            redis.eval(UNLOCK_LUA, Long.class, List.of(key), token);
        } catch (Exception e) {
            log.warn("释放锁失败 key={} : {}", key, e.getMessage());
        }
    }

    /** 带锁执行（推荐用法） */
    public <T> T withLock(String lockName, long waitSeconds, long leaseSeconds, Supplier<T> supplier) {
        String token = tryLock(lockName, waitSeconds, leaseSeconds);
        if (token == null) {
            throw new IllegalStateException("获取分布式锁失败：" + lockName);
        }
        try {
            return supplier.get();
        } finally {
            unlock(lockName, token);
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
