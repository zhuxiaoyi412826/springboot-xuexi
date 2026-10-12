package org.example.redis.cache;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * L1 + L2 + L3 三级缓存（本项目 Redis 部分的核心）。
 *
 * <pre>
 *   读：L1(Caffeine 本地) -> L2(Redis) -> L3(MySQL) -> 回填 L2、L1
 *   写：改数据库 -> 删 L2 -> 通过 Pub/Sub 广播 -> 所有节点删 L1（保证最终一致）
 * </pre>
 *
 * <p>三个经典问题的处理：
 * <ul>
 *   <li>穿透：布隆过滤器 + 空值缓存（{@code __NULL__} 占位）</li>
 *   <li>击穿：热点 key 逻辑过期 + 互斥锁重建（只有一个线程回源）</li>
 *   <li>雪崩：TTL 随机抖动</li>
 * </ul>
 */
@Slf4j
@Service
public class MultiLevelCacheService {

    /** 空值占位符：防止“查不存在的 id”反复打数据库 */
    public static final String NULL_PLACEHOLDER = "__NULL__";

    private static final String L2_PREFIX = "music:cache:%s:%s";
    private static final String SEPARATOR = "|";

    private final RedisService redis;
    private final ObjectMapper mapper;
    private final CacheProperties props;
    private final CacheStatsService stats;
    private final RedisLockService lockService;
    private final BloomFilterService bloom;
    private final RedisMessageListenerContainer listenerContainer;

    /** L1：进程内缓存，存的是 JSON 字符串（避免对象被业务代码修改） */
    private final Cache<String, String> l1;

    /** 缓存重建线程池（逻辑过期场景下异步回源） */
    private final ExecutorService rebuildPool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "cache-rebuild");
        t.setDaemon(true);
        return t;
    });

    public MultiLevelCacheService(RedisService redis,
                                  ObjectMapper mapper,
                                  CacheProperties props,
                                  CacheStatsService stats,
                                  RedisLockService lockService,
                                  BloomFilterService bloom,
                                  RedisMessageListenerContainer listenerContainer) {
        this.redis = redis;
        this.mapper = mapper;
        this.props = props;
        this.stats = stats;
        this.lockService = lockService;
        this.bloom = bloom;
        this.listenerContainer = listenerContainer;
        this.l1 = Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(Math.max(1, props.getL1TtlSeconds())))
                .recordStats()
                .build();
    }

    /** 订阅缓存失效广播：其他节点更新了数据，本节点的 L1 也要清掉 */
    @PostConstruct
    public void subscribeInvalidate() {
        try {
            listenerContainer.addMessageListener((message, pattern) -> {
                String body = new String(message.getBody());
                String[] arr = body.split("\\" + SEPARATOR);
                if (arr.length == 2) {
                    l1.invalidate(l1Key(arr[0], arr[1]));
                    log.debug("收到缓存失效广播，清理本地 L1：{}", body);
                }
            }, new ChannelTopic(RedisKeys.CHANNEL_CACHE_INVALIDATE));
        } catch (Exception e) {
            log.warn("订阅缓存失效频道失败（不影响启动）：{}", e.getMessage());
        }
    }

    // ==================== 对外 API ====================

    public <T> T get(String name, String key, Class<T> type, Supplier<T> loader) {
        return get(name, key, mapper.getTypeFactory().constructType(type),
                Duration.ofSeconds(props.getL2TtlSeconds()), loader, null, false);
    }

    public <T> T get(String name, String key, Class<T> type, Duration l2Ttl, Supplier<T> loader) {
        return get(name, key, mapper.getTypeFactory().constructType(type), l2Ttl, loader, null, false);
    }

    /** 需要序列化成 List/Map 等泛型结构时用这个 */
    public <T> T get(String name, String key, JavaType javaType, Duration l2Ttl, Supplier<T> loader) {
        return get(name, key, javaType, l2Ttl, loader, null, false);
    }

    /** 带布隆过滤器防穿透（bloomName 为空则不启用） */
    public <T> T get(String name, String key, Class<T> type, Duration l2Ttl, Supplier<T> loader, String bloomName) {
        return get(name, key, mapper.getTypeFactory().constructType(type), l2Ttl, loader, bloomName, false);
    }

    /**
     * 热点 key：逻辑过期 + 互斥锁异步重建。
     * 物理上永不过期（TTL 给一个很长的兜底值），靠 JSON 里的 expireAt 判断是否需要刷新，
     * 刷新期间先返回旧数据，用户无感知，也不会有并发一起打数据库。
     */
    public <T> T getHot(String name, String key, Class<T> type, Supplier<T> loader) {
        return get(name, key, mapper.getTypeFactory().constructType(type),
                Duration.ofSeconds(props.getLogicalExpireSeconds() + 86_400), loader, null, true);
    }

    /** 删除缓存（L2 + 本地 L1 + 广播其他节点） */
    public void evict(String name, String key) {
        redis.delete(l2Key(name, key));
        l1.invalidate(l1Key(name, key));
        try {
            redis.publish(RedisKeys.CHANNEL_CACHE_INVALIDATE, name + SEPARATOR + key);
        } catch (Exception e) {
            log.warn("发布缓存失效消息失败：{}", e.getMessage());
        }
    }

    /** 只清理本节点的 L1（收到广播时调用） */
    public void evictLocal(String name, String key) {
        l1.invalidate(l1Key(name, key));
    }

    /** 清空某个缓存名下的所有 L2 key（后台管理用） */
    public long clear(String name) {
        var keys = redis.scan(String.format(L2_PREFIX, name, "*"), 500);
        if (!keys.isEmpty()) {
            redis.delete(List.copyOf(keys));
        }
        l1.invalidateAll();
        return keys.size();
    }

    public Map<String, Object> stats(String name) {
        return stats.stats(name);
    }

    public Map<String, Object> statsAll() {
        return stats.all();
    }

    public void resetStats() {
        stats.reset();
    }

    /** L1 命中率等原生指标 */
    public Map<String, Object> l1Info() {
        var s = l1.stats();
        return Map.of(
                "size", l1.estimatedSize(),
                "hitCount", s.hitCount(),
                "missCount", s.missCount(),
                "hitRate", String.format("%.2f%%", s.hitRate() * 100),
                "evictionCount", s.evictionCount()
        );
    }

    // ==================== 内部实现 ====================

    private <T> T get(String name, String key, JavaType javaType, Duration l2Ttl,
                      Supplier<T> loader, String bloomName, boolean logicalExpire) {
        // 0) 布隆过滤器：一定不存在就直接返回，避免打库（防穿透）
        if (props.isBloomEnabled() && bloomName != null && !bloom.mightContain(bloomName, key)) {
            stats.hitNull(name);
            log.debug("布隆过滤器拦截：{}/{} 不存在", bloomName, key);
            return null;
        }

        String l1Key = l1Key(name, key);
        // 1) L1 本地缓存
        if (props.isL1Enabled()) {
            String json = l1.getIfPresent(l1Key);
            if (json != null) {
                stats.hit(CacheLevel.L1, name);
                return decode(json, javaType, logicalExpire, name, key);
            }
        }

        // 2) L2 Redis
        String l2Key = l2Key(name, key);
        if (props.isL2Enabled()) {
            String json = redis.get(l2Key);
            if (json != null) {
                if (NULL_PLACEHOLDER.equals(json)) {
                    stats.hitNull(name);
                    return null;
                }
                stats.hit(CacheLevel.L2, name);
                putL1(l1Key, json);
                T value = decode(json, javaType, logicalExpire, name, key);
                // 逻辑过期：异步刷新，先返回旧值（防击穿）
                if (logicalExpire && isLogicalExpired(json)) {
                    asyncRebuild(name, key, l2Key, loader);
                }
                return value;
            }
        }

        // 3) L3 回源（MySQL）
        if (!props.isL3Enabled()) {
            return null;
        }
        stats.hit(CacheLevel.L3, name);
        return rebuild(name, key, l2Key, javaType, l2Ttl, loader, logicalExpire);
    }

    /** 回源重建：互斥锁保证只有一个线程查库 */
    private <T> T rebuild(String name, String key, String l2Key, JavaType javaType,
                          Duration l2Ttl, Supplier<T> loader, boolean logicalExpire) {
        String lockName = "rebuild:" + name + ":" + key;
        String token = lockService.tryLock(lockName, props.getLockWaitSeconds(), 10);
        try {
            // 双检：等待锁期间可能已有线程写好缓存
            if (props.isL2Enabled()) {
                String json = redis.get(l2Key);
                if (json != null && !NULL_PLACEHOLDER.equals(json)) {
                    return decode(json, javaType, logicalExpire, name, key);
                }
            }
            T data = loader.get();
            if (data == null) {
                // 空值缓存：短时间内不再打数据库（防穿透）
                if (props.isL2Enabled()) {
                    redis.setEx(l2Key, NULL_PLACEHOLDER, Duration.ofSeconds(props.getNullTtlSeconds()));
                }
                return null;
            }
            String json = encode(data, logicalExpire);
            if (props.isL2Enabled()) {
                redis.setEx(l2Key, json, ttlWithJitter(l2Ttl));
            }
            putL1(l1Key(name, key), json);
            return data;
        } finally {
            lockService.unlock(lockName, token);
        }
    }

    /** 逻辑过期后异步重建，不阻塞当前请求 */
    private <T> void asyncRebuild(String name, String key, String l2Key, Supplier<T> loader) {
        String lockName = "rebuild:" + name + ":" + key;
        rebuildPool.submit(() -> {
            String token = lockService.tryLock(lockName, 0, 10);
            if (token == null) {
                return;
            }
            try {
                T data = loader.get();
                if (data != null) {
                    String json = encode(data, true);
                    redis.setEx(l2Key, json, Duration.ofSeconds(props.getLogicalExpireSeconds() + 86_400));
                    putL1(l1Key(name, key), json);
                    log.debug("热点缓存异步重建完成：{}/{}", name, key);
                }
            } catch (Exception e) {
                log.warn("异步重建缓存失败：{}", e.getMessage());
            } finally {
                lockService.unlock(lockName, token);
            }
        });
    }

    /** 过期时间加随机抖动，防止同一批 key 同时失效（防雪崩） */
    private Duration ttlWithJitter(Duration ttl) {
        long jitter = props.getL2JitterSeconds() <= 0 ? 0 : ThreadLocalRandom.current().nextLong(props.getL2JitterSeconds());
        return ttl.plusSeconds(jitter);
    }

    private void putL1(String l1Key, String json) {
        if (props.isL1Enabled()) {
            l1.put(l1Key, json);
        }
    }

    private String encode(Object data, boolean logicalExpire) {
        try {
            if (!logicalExpire) {
                return mapper.writeValueAsString(data);
            }
            ObjectNode node = mapper.createObjectNode();
            node.putPOJO("data", data);
            node.put("expireAt", System.currentTimeMillis() + props.getLogicalExpireSeconds() * 1000);
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new IllegalStateException("缓存序列化失败", e);
        }
    }

    private <T> T decode(String json, JavaType javaType, boolean logicalExpire, String name, String key) {
        try {
            String real = json;
            if (logicalExpire) {
                var node = mapper.readTree(json);
                real = node.get("data").toString();
            }
            return mapper.readValue(real, javaType);
        } catch (Exception e) {
            // 缓存与代码不一致时直接删掉，下次自动重建（缓存降级的兜底）
            log.warn("缓存反序列化失败，已删除 {}/{} : {}", name, key, e.getMessage());
            redis.delete(l2Key(name, key));
            l1.invalidate(l1Key(name, key));
            return null;
        }
    }

    private boolean isLogicalExpired(String json) {
        try {
            var node = mapper.readTree(json);
            long expireAt = node.path("expireAt").asLong(0);
            return expireAt > 0 && System.currentTimeMillis() > expireAt;
        } catch (Exception e) {
            return true;
        }
    }

    private static String l1Key(String name, String key) {
        return name + SEPARATOR + key;
    }

    private static String l2Key(String name, String key) {
        return String.format(L2_PREFIX, name, key);
    }
}
