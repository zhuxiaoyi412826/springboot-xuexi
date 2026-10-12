package org.example.redis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.cache.BloomFilterService;
import org.example.redis.cache.DelayQueueService;
import org.example.redis.cache.IdGeneratorService;
import org.example.redis.cache.RateLimiterService;
import org.example.redis.cache.RedisLockService;
import org.example.redis.common.Result;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis 实验室：把每种数据结构都做成一个可以直接点、直接看的接口，
 * 配合 redis-cli 观察效果（推荐一边调接口一边 redis-cli MONITOR）。
 *
 * <p>接口全部免登录，方便用浏览器/curl 直接玩。
 */
@RestController
@RequestMapping("/api/lab")
@RequiredArgsConstructor
@Tag(name = "Redis 实验室")
public class LabController {

    private final RedisService redis;
    private final StringRedisTemplate template;
    private final RedisLockService lockService;
    private final RateLimiterService rateLimiter;
    private final DelayQueueService delayQueue;
    private final IdGeneratorService idGenerator;
    private final BloomFilterService bloom;

    private static Map<String, Object> data(String tip, Object value) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tip", tip);
        m.put("value", value);
        return m;
    }

    // ==================== String ====================
    @GetMapping("/string/set")
    @Operation(summary = "String：SET/SETEX（缓存、配置开关、验证码）")
    public Result<Map<String, Object>> stringSet(@RequestParam String k, @RequestParam String v,
                                                 @RequestParam(required = false) Long ttl) {
        String key = RedisKeys.PREFIX + "lab:" + k;
        if (ttl != null && ttl > 0) {
            redis.setEx(key, v, java.time.Duration.ofSeconds(ttl));
        } else {
            redis.set(key, v);
        }
        return Result.ok(data("写入成功，redis-cli 执行：GET " + key, redis.get(key)));
    }

    @GetMapping("/string/get")
    @Operation(summary = "String：GET")
    public Result<Map<String, Object>> stringGet(@RequestParam String k) {
        return Result.ok(data("key=" + RedisKeys.PREFIX + "lab:" + k, redis.get(RedisKeys.PREFIX + "lab:" + k)));
    }

    @GetMapping("/string/incr")
    @Operation(summary = "String：INCRBY（计数器：播放量、点赞数）")
    public Result<Map<String, Object>> incr(@RequestParam String k, @RequestParam(defaultValue = "1") long delta) {
        String key = RedisKeys.PREFIX + "lab:counter:" + k;
        return Result.ok(data("计数器（原子自增，并发安全）", redis.incrBy(key, delta)));
    }

    @GetMapping("/string/setnx")
    @Operation(summary = "String：SETNX（分布式锁/幂等的基础）")
    public Result<Map<String, Object>> setnx(@RequestParam String k, @RequestParam(defaultValue = "1") String v,
                                             @RequestParam(defaultValue = "30") long ttl) {
        String key = RedisKeys.PREFIX + "lab:nx:" + k;
        Boolean ok = redis.setIfAbsent(key, v, java.time.Duration.ofSeconds(ttl));
        return Result.ok(data("第一次返回 true，30 秒内再点返回 false", ok));
    }

    // ==================== Hash ====================
    @GetMapping("/hash/set")
    @Operation(summary = "Hash：HSET（对象属性：用户信息、歌单计数）")
    public Result<Map<String, Object>> hashSet(@RequestParam String k, @RequestParam String f, @RequestParam String v) {
        String key = RedisKeys.PREFIX + "lab:hash:" + k;
        redis.hSet(key, f, v);
        return Result.ok(data("HGETALL " + key, redis.hGetAll(key)));
    }

    @GetMapping("/hash/incr")
    @Operation(summary = "Hash：HINCRBY（对象里的某个字段自增）")
    public Result<Map<String, Object>> hashIncr(@RequestParam String k, @RequestParam String f,
                                                @RequestParam(defaultValue = "1") long d) {
        String key = RedisKeys.PREFIX + "lab:hash:" + k;
        return Result.ok(data("字段 " + f + " 自增后", redis.hIncrBy(key, f, d)));
    }

    // ==================== List ====================
    @GetMapping("/list/push")
    @Operation(summary = "List：LPUSH + LTRIM（最近播放、搜索历史、队列）")
    public Result<Map<String, Object>> listPush(@RequestParam String k, @RequestParam String v,
                                                @RequestParam(defaultValue = "10") int max) {
        String key = RedisKeys.PREFIX + "lab:list:" + k;
        redis.lPushAndTrim(key, v, max);
        return Result.ok(data("只保留最近 " + max + " 条", redis.lRange(key, 0, -1)));
    }

    @GetMapping("/list/pop")
    @Operation(summary = "List：RPOP（队列出队）")
    public Result<Map<String, Object>> listPop(@RequestParam String k) {
        return Result.ok(data("出队元素", redis.rPop(RedisKeys.PREFIX + "lab:list:" + k)));
    }

    // ==================== Set ====================
    @GetMapping("/set/add")
    @Operation(summary = "Set：SADD（点赞用户、收藏、标签，天然去重）")
    public Result<Map<String, Object>> setAdd(@RequestParam String k, @RequestParam String m) {
        String key = RedisKeys.PREFIX + "lab:set:" + k;
        redis.sAdd(key, m);
        return Result.ok(data("成员数 SCARD=" + redis.sCard(key), redis.sMembers(key)));
    }

    @GetMapping("/set/inter")
    @Operation(summary = "Set：SINTER（共同喜欢、共同好友）")
    public Result<Map<String, Object>> setInter(@RequestParam String k1, @RequestParam String k2) {
        var r = redis.sInter(RedisKeys.PREFIX + "lab:set:" + k1, RedisKeys.PREFIX + "lab:set:" + k2);
        return Result.ok(data("交集", r));
    }

    // ==================== ZSet ====================
    @GetMapping("/zset/add")
    @Operation(summary = "ZSet：ZADD（排行榜）")
    public Result<Map<String, Object>> zsetAdd(@RequestParam(defaultValue = "rank") String k,
                                               @RequestParam String m, @RequestParam double score) {
        String key = RedisKeys.PREFIX + "lab:zset:" + k;
        redis.zAdd(key, m, score);
        return Result.ok(data("带分数的榜单", redis.zRevRangeWithScores(key, 0, 9)));
    }

    @GetMapping("/zset/incr")
    @Operation(summary = "ZSet：ZINCRBY（播放一次加一分）")
    public Result<Map<String, Object>> zsetIncr(@RequestParam(defaultValue = "rank") String k,
                                                @RequestParam String m, @RequestParam double delta) {
        String key = RedisKeys.PREFIX + "lab:zset:" + k;
        return Result.ok(data("加分后的 score", redis.zIncrBy(key, m, delta)));
    }

    @GetMapping("/zset/top")
    @Operation(summary = "ZSet：ZREVRANGE（TopN）")
    public Result<Map<String, Object>> zsetTop(@RequestParam(defaultValue = "rank") String k,
                                               @RequestParam(defaultValue = "10") int n) {
        return Result.ok(data("TopN", redis.zRevRangeWithScores(RedisKeys.PREFIX + "lab:zset:" + k, 0, n - 1)));
    }

    // ==================== Bitmap ====================
    @GetMapping("/bitmap/set")
    @Operation(summary = "Bitmap：SETBIT（签到）")
    public Result<Map<String, Object>> bitmapSet(@RequestParam String k, @RequestParam long offset,
                                                 @RequestParam(defaultValue = "true") boolean value) {
        String key = RedisKeys.PREFIX + "lab:bitmap:" + k;
        redis.setBit(key, offset, value);
        return Result.ok(data("BITCOUNT（1 的个数）=" + redis.bitCount(key) + "，占用内存极小", value));
    }

    @GetMapping("/bitmap/get")
    @Operation(summary = "Bitmap：GETBIT")
    public Result<Map<String, Object>> bitmapGet(@RequestParam String k, @RequestParam long offset) {
        return Result.ok(data("该位的值", redis.getBit(RedisKeys.PREFIX + "lab:bitmap:" + k, offset)));
    }

    // ==================== HyperLogLog ====================
    @GetMapping("/hll/add")
    @Operation(summary = "HyperLogLog：PFADD（UV 去重计数，固定 12KB）")
    public Result<Map<String, Object>> hllAdd(@RequestParam String k, @RequestParam String v) {
        String key = RedisKeys.PREFIX + "lab:hll:" + k;
        redis.pfAdd(key, v);
        return Result.ok(data("PFCOUNT（去重近似值，误差 0.81%）", redis.pfCount(key)));
    }

    // ==================== Geo ====================
    @GetMapping("/geo/add")
    @Operation(summary = "Geo：GEOADD（附近的歌友）")
    public Result<Map<String, Object>> geoAdd(@RequestParam String k, @RequestParam String member,
                                              @RequestParam double lng, @RequestParam double lat) {
        String key = RedisKeys.PREFIX + "lab:geo:" + k;
        redis.geoAdd(key, new org.springframework.data.geo.Point(lng, lat), member);
        return Result.ok(data("已写入坐标", redis.geoPos(key, member)));
    }

    @GetMapping("/geo/near")
    @Operation(summary = "Geo：GEORADIUS（半径查询）")
    public Result<Map<String, Object>> geoNear(@RequestParam String k, @RequestParam double lng,
                                               @RequestParam double lat,
                                               @RequestParam(defaultValue = "100") double radiusKm) {
        String key = RedisKeys.PREFIX + "lab:geo:" + k;
        var r = redis.geoNear(key, new org.springframework.data.geo.Point(lng, lat), radiusKm, 20);
        List<Map<String, Object>> list = new ArrayList<>();
        r.forEach(g -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("member", g.getContent().getName());
            m.put("distanceKm", g.getDistance().getValue());
            list.add(m);
        });
        return Result.ok(data("半径 " + radiusKm + " km 内的成员", list));
    }

    // ==================== Stream ====================
    @PostMapping("/stream/add")
    @Operation(summary = "Stream：XADD（事件流水，可回溯、支持消费组）")
    public Result<Map<String, Object>> streamAdd(@RequestParam(defaultValue = "play") String k,
                                                 @RequestBody Map<String, String> fields) {
        String key = RedisKeys.PREFIX + "lab:stream:" + k;
        var id = redis.xAdd(key, fields);
        return Result.ok(data("消息 id", id == null ? null : id.toString()));
    }

    @GetMapping("/stream/read")
    @Operation(summary = "Stream：XREAD（从头读 n 条）")
    public Result<Map<String, Object>> streamRead(@RequestParam(defaultValue = "play") String k,
                                                  @RequestParam(defaultValue = "10") int n) {
        String key = RedisKeys.PREFIX + "lab:stream:" + k;
        var records = redis.xRead(key, "0-0", n);
        List<Map<String, Object>> list = new ArrayList<>();
        records.forEach(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId().toString());
            m.put("value", r.getValue());
            list.add(m);
        });
        return Result.ok(data("共 " + records.size() + " 条", list));
    }

    // ==================== Pub/Sub ====================
    @GetMapping("/pubsub/pub")
    @Operation(summary = "Pub/Sub：PUBLISH（广播，订阅者不在线就收不到）")
    public Result<Map<String, Object>> publish(@RequestParam String msg) {
        redis.publish(RedisKeys.CHANNEL_NOTICE, msg);
        return Result.ok(data("已广播到频道 " + RedisKeys.CHANNEL_NOTICE + "，管理端「通知」页能看到", msg));
    }

    // ==================== 分布式锁 ====================
    @GetMapping("/lock")
    @Operation(summary = "分布式锁：SET NX PX + Lua 释放（并发点两次看效果）")
    public Result<Map<String, Object>> lock(@RequestParam(defaultValue = "demo") String name,
                                            @RequestParam(defaultValue = "10") long lease) {
        String token = lockService.tryLock("lab:" + name, 2, lease);
        if (token == null) {
            return Result.ok(data("没拿到锁（说明别的线程正持有）", false));
        }
        try {
            return Result.ok(data("拿到锁，value=" + token.substring(0, 8) + "…，"
                    + lease + " 秒内再点会失败", true));
        } finally {
            lockService.unlock("lab:" + name, token);
        }
    }

    // ==================== Lua 限流 ====================
    @GetMapping("/ratelimit")
    @Operation(summary = "Lua 令牌桶限流：连续刷新直到返回 0 就是被限流")
    public Result<Map<String, Object>> rateLimit(@RequestParam(defaultValue = "demo") String biz,
                                                 @RequestParam(defaultValue = "5") int capacity,
                                                 @RequestParam(defaultValue = "1") int refill,
                                                 @RequestParam(defaultValue = "1") int n) {
        boolean allowed = rateLimiter.allow("lab:" + biz, capacity, refill, n);
        return Result.ok(data("1=放行 0=限流", allowed ? 1 : 0));
    }

    // ==================== Pipeline ====================
    @GetMapping("/pipeline")
    @Operation(summary = "Pipeline：批量写入对比（网络往返次数决定性能）")
    public Result<Map<String, Object>> pipeline(@RequestParam(defaultValue = "200") int n) {
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            redis.set(RedisKeys.PREFIX + "lab:pipe:normal:" + i, "v" + i);
        }
        long normal = System.currentTimeMillis() - t1;

        long t2 = System.currentTimeMillis();
        redis.pipeline(t -> {
            for (int i = 0; i < n; i++) {
                redis.set(RedisKeys.PREFIX + "lab:pipe:batch:" + i, "v" + i);
            }
        });
        long batch = System.currentTimeMillis() - t2;

        Map<String, Object> value = new LinkedHashMap<>();
        value.put("普通逐条写(ms)", normal);
        value.put("pipeline 批量写(ms)", batch);
        return Result.ok(data("写入 " + n + " 个 key 的耗时对比", value));
    }

    // ==================== 事务 ====================
    @GetMapping("/tx")
    @Operation(summary = "事务：MULTI / EXEC（一起成功一起排队，注意 Redis 事务不回滚）")
    public Result<Map<String, Object>> tx() {
        List<Object> result = template.execute(new org.springframework.data.redis.core.SessionCallback<List<Object>>() {
            @SuppressWarnings("unchecked")
            @Override
            public List<Object> execute(org.springframework.data.redis.core.RedisOperations operations) {
                operations.multi();
                operations.opsForValue().set(RedisKeys.PREFIX + "lab:tx:a", "1");
                operations.opsForValue().set(RedisKeys.PREFIX + "lab:tx:b", "2");
                return operations.exec();
            }
        });
        return Result.ok(data("EXEC 返回结果", result));
    }

    // ==================== 延迟队列 ====================
    @GetMapping("/delay/add")
    @Operation(summary = "延迟队列：投递任务（payload 形如 song:1:online）")
    public Result<Map<String, Object>> delayAdd(@RequestParam String payload,
                                                @RequestParam(defaultValue = "30") long seconds) {
        delayQueue.push(payload, seconds);
        return Result.ok(data(seconds + " 秒后执行，后台每 10 秒轮询一次", delayQueue.size()));
    }

    @GetMapping("/delay/list")
    @Operation(summary = "延迟队列：查看待执行任务")
    public Result<Map<String, Object>> delayList() {
        return Result.ok(data("待执行任务", delayQueue.peek(20)));
    }

    // ==================== 分布式 ID ====================
    @GetMapping("/id/next")
    @Operation(summary = "分布式 ID：INCR（原子、全局唯一）")
    public Result<Map<String, Object>> nextId(@RequestParam(defaultValue = "demo") String biz) {
        return Result.ok(data("下一个 id", idGenerator.nextId("lab:" + biz)));
    }

    @GetMapping("/id/range")
    @Operation(summary = "分布式 ID：INCRBY 号段模式（一次取一批，减少 Redis 访问）")
    public Result<Map<String, Object>> nextRange(@RequestParam(defaultValue = "demo") String biz,
                                                 @RequestParam(defaultValue = "10") int step) {
        return Result.ok(data("取到的 id 段", idGenerator.nextRange("lab:" + biz, step)));
    }

    // ==================== 布隆过滤器 ====================
    @GetMapping("/bloom/add")
    @Operation(summary = "布隆过滤器：加入元素 + 标记就绪")
    public Result<Map<String, Object>> bloomAdd(@RequestParam(defaultValue = "demo") String name,
                                                @RequestParam String value) {
        if (!bloom.isReady(name)) {
            bloom.init(name);
        }
        bloom.add(name, value);
        bloom.markReady(name);
        return Result.ok(data("已加入；说不存在就一定不存在，说存在可能有极小误判", true));
    }

    @GetMapping("/bloom/exists")
    @Operation(summary = "布隆过滤器：判断是否存在（false=一定不存在）")
    public Result<Map<String, Object>> bloomExists(@RequestParam(defaultValue = "demo") String name,
                                                   @RequestParam String value) {
        return Result.ok(data("mightContain", bloom.mightContain(name, value)));
    }

    // ==================== Key 管理 ====================
    @GetMapping("/keys")
    @Operation(summary = "SCAN 查看 key（生产用 SCAN，禁止 KEYS *）")
    public Result<Map<String, Object>> keys(@RequestParam(defaultValue = "music:*") String pattern,
                                            @RequestParam(defaultValue = "50") int count) {
        return Result.ok(data("匹配 " + pattern + " 的 key", redis.scan(pattern, count)));
    }

    @GetMapping("/ttl")
    @Operation(summary = "查看 key 剩余存活时间（秒）：-1 永不过期，-2 不存在")
    public Result<Map<String, Object>> ttl(@RequestParam String key) {
        return Result.ok(data("TTL", redis.ttl(key)));
    }

    @GetMapping("/del")
    @Operation(summary = "删除 key（只允许 music: 前缀）")
    public Result<Map<String, Object>> del(@RequestParam String key) {
        if (!key.startsWith(RedisKeys.PREFIX)) {
            return Result.ok(data("只允许删除 music: 前缀的 key", false));
        }
        return Result.ok(data("删除结果", redis.delete(key)));
    }
}
