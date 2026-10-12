package org.example.redis.cache;

import lombok.RequiredArgsConstructor;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * 自研布隆过滤器（基于 Redis Bitmap，不依赖 Redisson）。
 *
 * <p>作用：缓存穿透防护的第一道门——查询一个根本不存在的 id 时，
 * 布隆过滤器可以 O(1) 判定“一定不存在”，直接返回，不再打数据库。
 *
 * <p>特点：说“不存在”一定不存在；说“存在”可能误判（可控的误判率）。
 */
@Service
@RequiredArgsConstructor
public class BloomFilterService {

    /** 默认位数组长度：2^22 bit = 512KB（学习用，够演示） */
    private static final long DEFAULT_BITS = 1L << 22;
    private static final int DEFAULT_HASHES = 3;

    private final RedisService redis;

    /** 初始化（幂等） */
    public void init(String name) {
        init(name, DEFAULT_BITS, DEFAULT_HASHES);
    }

    public void init(String name, long bits, int hashes) {
        String cfgKey = cfgKey(name);
        redis.hSet(cfgKey, "bits", String.valueOf(bits));
        redis.hSet(cfgKey, "hashes", String.valueOf(hashes));
        redis.hSet(cfgKey, "ready", "0");
    }

    /**
     * 标记「过滤器已经灌好数据，可以开始拦截了」。
     * 非常重要：没有灌数据的布隆过滤器会把所有查询都判成“不存在”。
     */
    public void markReady(String name) {
        redis.hSet(cfgKey(name), "ready", "1");
    }

    public boolean isReady(String name) {
        Object ready = redis.hGet(cfgKey(name), "ready");
        return ready != null && "1".equals(ready.toString());
    }

    public void add(String name, String value) {
        long[] positions = positions(name, value);
        for (long p : positions) {
            redis.setBit(key(name), p, true);
        }
    }

    public boolean mightContain(String name, String value) {
        long[] positions = positions(name, value);
        for (long p : positions) {
            if (!Boolean.TRUE.equals(redis.getBit(key(name), p))) {
                return false;
            }
        }
        return true;
    }

    /** 已被加入过滤器的数量（近似，用 BITCOUNT 演示） */
    public long bitCount(String name) {
        Long n = redis.bitCount(key(name));
        return n == null ? 0L : n;
    }

    private long[] positions(String name, String value) {
        long bits = DEFAULT_BITS;
        int hashes = DEFAULT_HASHES;
        Object b = redis.hGet(cfgKey(name), "bits");
        Object h = redis.hGet(cfgKey(name), "hashes");
        if (b != null) {
            bits = Long.parseLong(b.toString());
        }
        if (h != null) {
            hashes = Integer.parseInt(h.toString());
        }
        long[] res = new long[hashes];
        long h1 = fnv1a(value, 0x811c9dc5L);
        long h2 = fnv1a(value, 0x01000193L);
        for (int i = 0; i < hashes; i++) {
            res[i] = Math.floorMod(h1 + (long) i * h2, bits);
        }
        return res;
    }

    private static long fnv1a(String value, long seed) {
        long hash = seed;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= (b & 0xff);
            hash *= 0x01000193L;
        }
        return hash & 0x7fffffffffffffffL;
    }

    private static String key(String name) {
        return String.format(RedisKeys.BLOOM, name);
    }

    private static String cfgKey(String name) {
        return key(name) + ":cfg";
    }
}
