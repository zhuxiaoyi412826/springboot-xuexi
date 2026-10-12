package org.example.redis.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.RecordId;

/**
 * Redis 全数据结构封装（String / Hash / List / Set / ZSet / Bitmap / HLL / Geo /
 * Stream / PubSub / Lua / Pipeline）。
 *
 * <p>统一走 StringRedisTemplate：key 和 value 都是 String，对象由调用方转 JSON，
 * 这样在 redis-cli 里可以直接 GET 查看，不会出现 JDK 序列化的乱码。
 */
@Component
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redis;

    // ==================== String ====================
    public void set(String key, String value) {
        redis.opsForValue().set(key, value);
    }

    public void setEx(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    /** SET key value NX PX ttl —— 分布式锁的基石 */
    public Boolean setIfAbsent(String key, String value, Duration ttl) {
        return redis.opsForValue().setIfAbsent(key, value, ttl);
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    public Long incr(String key) {
        return redis.opsForValue().increment(key);
    }

    public Long incrBy(String key, long delta) {
        return redis.opsForValue().increment(key, delta);
    }

    // ==================== 通用 ====================
    public Boolean expire(String key, Duration ttl) {
        return redis.expire(key, ttl);
    }

    public Long ttl(String key) {
        Long t = redis.getExpire(key);
        return t == null ? -2L : t;
    }

    public Boolean hasKey(String key) {
        return Boolean.TRUE.equals(redis.hasKey(key));
    }

    public Boolean delete(String key) {
        return redis.delete(key);
    }

    public Long delete(Collection<String> keys) {
        return redis.delete(keys);
    }

    public String type(String key) {
        var dataType = redis.type(key);
        return dataType == null ? null : dataType.code();
    }

    /** SCAN 匹配（生产环境禁止用 KEYS） */
    public Set<String> scan(String pattern, int count) {
        Set<String> result = new java.util.LinkedHashSet<>();
        try (var cursor = redis.scan(org.springframework.data.redis.core.ScanOptions.scanOptions()
                .match(pattern).count(count).build())) {
            cursor.forEachRemaining(result::add);
        }
        return result;
    }

    // ==================== Hash ====================
    public void hSet(String key, String field, String value) {
        redis.opsForHash().put(key, field, value);
    }

    public void hSetAll(String key, Map<String, String> map) {
        redis.opsForHash().putAll(key, map);
    }

    public Object hGet(String key, String field) {
        return redis.opsForHash().get(key, field);
    }

    public Map<Object, Object> hGetAll(String key) {
        return redis.opsForHash().entries(key);
    }

    public Long hIncrBy(String key, String field, long delta) {
        return redis.opsForHash().increment(key, field, delta);
    }

    public void hDel(String key, Object... fields) {
        redis.opsForHash().delete(key, fields);
    }

    // ==================== List ====================
    public Long lPush(String key, String value) {
        return redis.opsForList().leftPush(key, value);
    }

    /** 保留最近 n 条（最近播放/搜索历史都要裁剪） */
    public Long lPushAndTrim(String key, String value, int maxLen) {
        Long size = redis.opsForList().leftPush(key, value);
        redis.opsForList().trim(key, 0, maxLen - 1L);
        return size;
    }

    public List<String> lRange(String key, long start, long end) {
        List<String> list = redis.opsForList().range(key, start, end);
        return list == null ? List.of() : list;
    }

    public Long lLen(String key) {
        Long len = redis.opsForList().size(key);
        return len == null ? 0L : len;
    }

    public String rPop(String key) {
        return redis.opsForList().rightPop(key);
    }

    // ==================== Set ====================
    public Long sAdd(String key, String... members) {
        return redis.opsForSet().add(key, members);
    }

    public Boolean sIsMember(String key, String member) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(key, member));
    }

    public Set<String> sMembers(String key) {
        Set<String> set = redis.opsForSet().members(key);
        return set == null ? Set.of() : set;
    }

    public Long sRem(String key, String... members) {
        return redis.opsForSet().remove(key, members);
    }

    public Long sCard(String key) {
        Long n = redis.opsForSet().size(key);
        return n == null ? 0L : n;
    }

    /** 交集：共同喜欢这首歌的人 / 共同好友 */
    public Set<String> sInter(String key1, String key2) {
        Set<String> set = redis.opsForSet().intersect(key1, key2);
        return set == null ? Set.of() : set;
    }

    // ==================== ZSet ====================
    public Boolean zAdd(String key, String member, double score) {
        return Boolean.TRUE.equals(redis.opsForZSet().add(key, member, score));
    }

    public Double zIncrBy(String key, String member, double delta) {
        return redis.opsForZSet().incrementScore(key, member, delta);
    }

    public Double zScore(String key, String member) {
        return redis.opsForZSet().score(key, member);
    }

    public Long zCard(String key) {
        Long n = redis.opsForZSet().zCard(key);
        return n == null ? 0L : n;
    }

    public void zRem(String key, String... members) {
        redis.opsForZSet().remove(key, members);
    }

    /** 倒序取前 n 名（排行榜） */
    public List<String> zRevRange(String key, int start, int end) {
        Set<String> set = redis.opsForZSet().reverseRange(key, start, end);
        return set == null ? List.of() : new ArrayList<>(set);
    }

    /** 带分数的榜单 */
    public Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> zRevRangeWithScores(String key, int start, int end) {
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> set =
                redis.opsForZSet().reverseRangeWithScores(key, start, end);
        return set == null ? Set.of() : set;
    }

    /** 按分数区间取（延迟队列到期任务） */
    public Set<String> zRangeByScore(String key, double min, double max) {
        Set<String> set = redis.opsForZSet().rangeByScore(key, min, max);
        return set == null ? Set.of() : set;
    }

    // ==================== Bitmap ====================
    public Boolean setBit(String key, long offset, boolean value) {
        return redis.opsForValue().setBit(key, offset, value);
    }

    public Boolean getBit(String key, long offset) {
        return redis.opsForValue().getBit(key, offset);
    }

    public Long bitCount(String key) {
        return redis.execute((RedisCallback<Long>)
                con -> con.stringCommands().bitCount(key.getBytes()));
    }

    // ==================== HyperLogLog ====================
    public Long pfAdd(String key, String... values) {
        return redis.opsForHyperLogLog().add(key, values);
    }

    public Long pfCount(String key) {
        return redis.opsForHyperLogLog().size(key);
    }

    // ==================== Geo ====================
    public Long geoAdd(String key, Point point, String member) {
        return redis.opsForGeo().add(key, point, member);
    }

    public List<Point> geoPos(String key, String... members) {
        return redis.opsForGeo().position(key, members);
    }

    public Distance geoDist(String key, String m1, String m2) {
        return redis.opsForGeo().distance(key, m1, m2);
    }

    /** 附近的人/附近的电台 */
    public List<GeoResult<org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation<String>>> geoNear(
            String key, Point point, double radiusKm, int limit) {
        var results = redis.opsForGeo().radius(key, new Circle(point,
                new Distance(radiusKm, org.springframework.data.geo.Metrics.KILOMETERS)),
                org.springframework.data.redis.connection.RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeDistance().includeCoordinates().limit(limit).sortAscending());
        return results == null ? List.of() : results.getContent();
    }

    // ==================== Stream ====================
    /** XADD：写入播放事件等流水 */
    public RecordId xAdd(String key, Map<String, String> fields) {
        MapRecord<String, String, String> record = StreamRecords.newRecord()
                .ofMap(fields)
                .withStreamKey(key);
        return redis.opsForStream().add(record);
    }

    /** XREAD：从指定 id 往后读（不建消费者组，简单可靠） */
    public List<MapRecord<String, Object, Object>> xRead(String key, String startId, int count) {
        List<MapRecord<String, Object, Object>> records = redis.opsForStream().read(
                StreamReadOptions.empty().count(count),
                StreamOffset.create(key, ReadOffset.from(startId)));
        return records == null ? List.of() : records;
    }

    public Long xLen(String key) {
        Long len = redis.opsForStream().size(key);
        return len == null ? 0L : len;
    }

    // ==================== Pub/Sub ====================
    public void publish(String channel, String message) {
        redis.convertAndSend(channel, message);
    }

    // ==================== Lua 脚本 ====================
    public <T> T eval(String script, Class<T> resultType, List<String> keys, Object... args) {
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>(script, resultType);
        return redis.execute(redisScript, keys, args);
    }

    // ==================== Pipeline ====================
    /** 批量写入（缓存预热），减少 RTT */
    public List<Object> pipeline(java.util.function.Consumer<StringRedisTemplate> action) {
        return redis.executePipelined((RedisCallback<Object>) connection -> {
            action.accept(redis);
            return null;
        });
    }
}
