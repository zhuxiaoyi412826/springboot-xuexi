package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.cache.CacheProperties;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存与 Redis 运维面板（管理端用）。
 *
 * <p>提供：命中率统计、按缓存名清理、手动预热、L1/L2/L3 开关、key 浏览、Redis 服务器信息。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheManageService {

    private final MultiLevelCacheService cache;
    private final CacheProperties cacheProperties;
    private final RedisService redis;
    private final StringRedisTemplate template;
    private final WarmUpService warmUpService;

    /** 总览：缓存命中率 + L1 指标 + Redis 指标 */
    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("cacheStats", cache.statsAll());
        data.put("l1", cache.l1Info());
        data.put("levels", Map.of(
                "l1Enabled", cacheProperties.isL1Enabled(),
                "l2Enabled", cacheProperties.isL2Enabled(),
                "l3Enabled", cacheProperties.isL3Enabled(),
                "bloomEnabled", cacheProperties.isBloomEnabled()
        ));
        data.put("redis", redisInfo());
        return data;
    }

    /** Redis 服务器信息（INFO 命令） */
    public Map<String, Object> redisInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            Properties props = template.execute(
                    (org.springframework.data.redis.core.RedisCallback<Properties>) c -> c.info());
            if (props != null) {
                info.put("redis_version", props.getProperty("redis_version"));
                info.put("used_memory_human", props.getProperty("used_memory_human"));
                info.put("connected_clients", props.getProperty("connected_clients"));
                info.put("keyspace_hits", props.getProperty("keyspace_hits"));
                info.put("keyspace_misses", props.getProperty("keyspace_misses"));
                String hits = props.getProperty("keyspace_hits", "0");
                String misses = props.getProperty("keyspace_misses", "0");
                long h = Long.parseLong(hits);
                long m = Long.parseLong(misses);
                info.put("hitRate", (h + m) == 0 ? "0.00%" : String.format("%.2f%%", h * 100.0 / (h + m)));
            }
            Long size = template.execute(
                    (org.springframework.data.redis.core.RedisCallback<Long>) c -> c.dbSize());
            info.put("dbSize", size);
        } catch (Exception e) {
            info.put("error", e.getMessage());
        }
        return info;
    }

    /** 按缓存名清理 */
    public long clear(String name) {
        return cache.clear(name);
    }

    /** 手动预热 */
    public String warm() {
        warmUpService.warmUp();
        return "预热完成";
    }

    /** 动态开关缓存层级（演示用，重启后回到配置文件的值） */
    public Map<String, Object> switchLevel(String level, boolean enabled) {
        switch (level.toLowerCase()) {
            case "l1" -> cacheProperties.setL1Enabled(enabled);
            case "l2" -> cacheProperties.setL2Enabled(enabled);
            case "l3" -> cacheProperties.setL3Enabled(enabled);
            case "bloom" -> cacheProperties.setBloomEnabled(enabled);
            default -> throw new IllegalArgumentException("level 只能是 l1/l2/l3/bloom");
        }
        log.info("缓存开关变更：{}={}", level, enabled);
        return Map.of(level, enabled);
    }

    /** 浏览 key（SCAN，不用 KEYS） */
    public List<Map<String, Object>> keys(String pattern, int count) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String key : redis.scan(pattern, count)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", key);
            m.put("type", redis.type(key));
            m.put("ttl", redis.ttl(key));
            list.add(m);
        }
        return list;
    }

    /** 删除某个 key（管理端排查用） */
    public boolean deleteKey(String key) {
        if (!key.startsWith(RedisKeys.PREFIX)) {
            throw new IllegalArgumentException("只允许删除 music: 前缀的 key，避免误删");
        }
        return Boolean.TRUE.equals(redis.delete(key));
    }

    public void resetStats() {
        cache.resetStats();
    }
}
