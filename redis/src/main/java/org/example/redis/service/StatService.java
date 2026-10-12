package org.example.redis.service;

import lombok.RequiredArgsConstructor;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统计：UV（HyperLogLog）+ 日活（Bitmap）+ Geo（附近的人）。
 *
 * <p>HyperLogLog：统计「今天有多少个不同的人听过歌」只需要 12KB 内存，
 * 误差约 0.81%，用 Set 存同样的数据可能要几十 MB。
 */
@Service
@RequiredArgsConstructor
public class StatService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RedisService redis;

    /** 记录一个访客（用 userId 或 ip） */
    public void addUv(String identity) {
        String day = LocalDate.now().format(YMD);
        redis.pfAdd(String.format(RedisKeys.UV, day), identity);
    }

    /** 今日 UV（去重近似值） */
    public long uv(String day) {
        String d = day == null || day.isBlank() ? LocalDate.now().format(YMD) : day;
        Long n = redis.pfCount(String.format(RedisKeys.UV, d));
        return n == null ? 0L : n;
    }

    /** 今日活跃用户数（Bitmap BITCOUNT，精确） */
    public long activeCount(String day) {
        String d = day == null || day.isBlank() ? LocalDate.now().format(YMD) : day;
        Long n = redis.bitCount(String.format(RedisKeys.ACTIVE, d));
        return n == null ? 0L : n;
    }

    // ==================== Geo：附近的歌友 ====================

    public void reportLocation(Long userId, double lng, double lat) {
        redis.geoAdd(RedisKeys.GEO_LISTENER,
                new org.springframework.data.geo.Point(lng, lat), String.valueOf(userId));
    }

    public Map<String, Object> nearBy(double lng, double lat, double radiusKm, int limit) {
        var results = redis.geoNear(RedisKeys.GEO_LISTENER,
                new org.springframework.data.geo.Point(lng, lat), radiusKm, limit);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("center", Map.of("lng", lng, "lat", lat));
        data.put("radiusKm", radiusKm);
        data.put("members", results.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", r.getContent().getName());
            m.put("distanceKm", r.getDistance().getValue());
            m.put("point", Map.of("lng", r.getContent().getPoint().getX(), "lat", r.getContent().getPoint().getY()));
            return m;
        }).toList());
        return data;
    }
}
