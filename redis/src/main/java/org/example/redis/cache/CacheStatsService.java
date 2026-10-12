package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存命中率统计（知识点：没有监控的缓存等于盲开）。
 *
 * <p>本地用 AtomicLong 累加，同时把累计值写一份到 Redis Hash，方便多实例汇总与后台查看。
 */
@Service
@RequiredArgsConstructor
public class CacheStatsService {

    private final RedisService redis;
    private final Map<String, AtomicLong> localL1 = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localL2 = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localL3 = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> localNull = new ConcurrentHashMap<>();

    private static AtomicLong counter(Map<String, AtomicLong> map, String name) {
        return map.computeIfAbsent(name, k -> new AtomicLong());
    }

    public void hit(CacheLevel level, String name) {
        switch (level) {
            case L1 -> {
                counter(localL1, name).incrementAndGet();
                redis.hIncrBy(String.format(RedisKeys.CACHE_STATS, name), "l1", 1);
            }
            case L2 -> {
                counter(localL2, name).incrementAndGet();
                redis.hIncrBy(String.format(RedisKeys.CACHE_STATS, name), "l2", 1);
            }
            case L3 -> {
                counter(localL3, name).incrementAndGet();
                redis.hIncrBy(String.format(RedisKeys.CACHE_STATS, name), "l3", 1);
            }
        }
    }

    /** 命中“空值占位”（防穿透生效） */
    public void hitNull(String name) {
        counter(localNull, name).incrementAndGet();
        redis.hIncrBy(String.format(RedisKeys.CACHE_STATS, name), "null", 1);
    }

    public Map<String, Object> stats(String name) {
        long l1 = counter(localL1, name).get();
        long l2 = counter(localL2, name).get();
        long l3 = counter(localL3, name).get();
        long nul = counter(localNull, name).get();
        long total = l1 + l2 + l3;
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cache", name);
        m.put("l1Hit", l1);
        m.put("l2Hit", l2);
        m.put("l3Query", l3);
        m.put("nullHit", nul);
        m.put("total", total);
        m.put("hitRate", total == 0 ? "0.00%" : String.format("%.2f%%", (l1 + l2) * 100.0 / total));
        return m;
    }

    /** 汇总所有已知缓存名（本地 + Redis 里的） */
    public Map<String, Object> all() {
        Map<String, Object> result = new LinkedHashMap<>();
        for (String name : localL1.keySet()) {
            result.put(name, stats(name));
        }
        for (String name : localL2.keySet()) {
            result.putIfAbsent(name, stats(name));
        }
        for (String name : localL3.keySet()) {
            result.putIfAbsent(name, stats(name));
        }
        long total = 0;
        long hit = 0;
        for (Object v : result.values()) {
            Map<?, ?> m = (Map<?, ?>) v;
            long t = ((Number) m.get("total")).longValue();
            long l3 = ((Number) m.get("l3Query")).longValue();
            total += t;
            hit += (t - l3);
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalRequests", total);
        summary.put("totalHit", hit);
        summary.put("hitRate", total == 0 ? "0.00%" : String.format("%.2f%%", hit * 100.0 / total));
        result.put("_summary", summary);
        return result;
    }

    public void reset() {
        localL1.clear();
        localL2.clear();
        localL3.clear();
        localNull.clear();
    }
}
