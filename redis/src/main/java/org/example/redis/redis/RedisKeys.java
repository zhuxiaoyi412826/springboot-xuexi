package org.example.redis.redis;

/**
 * 全站 Redis key 命名规范（统一前缀 music:，用 : 分层）。
 *
 * <p>为什么要统一维护 key？
 * <ul>
 *   <li>避免各处硬编码拼错、便于批量清理与统计</li>
 *   <li>key 设计是 Redis 使用的第一件事：{业务}:{对象}:{id}:{属性}</li>
 * </ul>
 */
public final class RedisKeys {

    private RedisKeys() {
    }

    public static final String PREFIX = "music:";

    // ---------- 缓存（L2） ----------
    /** 歌曲详情：String(JSON) */
    public static final String SONG = "music:song:%d";
    /** 歌曲列表（首页/分页/分类）：String(JSON) */
    public static final String SONG_LIST = "music:song:list:%s";
    /** 歌单详情：Hash */
    public static final String PLAYLIST = "music:playlist:%d";
    /** 歌单里的歌曲 id 列表：List */
    public static final String PLAYLIST_SONGS = "music:playlist:songs:%d";
    /** 用户信息：Hash */
    public static final String USER = "music:user:%d";
    /** 搜索结果：String(JSON)，短 TTL */
    public static final String SEARCH = "music:search:%s";

    // ---------- 计数（String INCR） ----------
    public static final String CNT_PLAY = "music:cnt:play:%d";
    public static final String CNT_LIKE = "music:cnt:like:%d";
    public static final String CNT_VIEW = "music:cnt:view:%d";

    // ---------- 排行榜（ZSet） ----------
    public static final String RANK_HOT = "music:rank:hot";
    public static final String RANK_NEW = "music:rank:new";
    /** 热搜词 */
    public static final String RANK_SEARCH = "music:rank:search";

    // ---------- 集合（Set） ----------
    public static final String LIKE_USERS = "music:like:users:%d";
    public static final String FAV_USERS = "music:fav:users:%d";
    public static final String FAV_SONGS = "music:fav:songs:%d";
    public static final String ONLINE_USERS = "music:online:users";

    // ---------- 列表（List） ----------
    public static final String RECENT_PLAY = "music:recent:%d";
    public static final String SEARCH_HISTORY = "music:history:%d";
    /** 待审核歌单队列 */
    public static final String AUDIT_QUEUE = "music:queue:audit:playlist";

    // ---------- Bitmap ----------
    public static final String SIGN = "music:sign:%d:%s";     // %d=userId %s=yyyyMM
    public static final String ACTIVE = "music:active:%s";    // %s=yyyyMMdd 日活

    // ---------- HyperLogLog ----------
    public static final String UV = "music:uv:%s";            // %s=yyyyMMdd

    // ---------- Geo ----------
    public static final String GEO_LISTENER = "music:geo:listener";

    // ---------- 分布式锁 / 幂等 / 限流 ----------
    public static final String LOCK = "music:lock:%s";
    public static final String LOCK_CACHE_REBUILD = "music:lock:rebuild:%s";
    public static final String IDEMPOTENT = "music:idem:%s";
    public static final String RATE_LIMIT = "music:rate:%s";

    // ---------- Stream / PubSub / 延迟队列 ----------
    public static final String STREAM_PLAY = "music:stream:play";
    public static final String CHANNEL_CACHE_INVALIDATE = "music:channel:cache:invalidate";
    public static final String CHANNEL_NOTICE = "music:channel:notice";
    public static final String DELAY_QUEUE = "music:delay:queue";

    // ---------- 布隆过滤器（自研，基于 Bitmap） ----------
    public static final String BLOOM = "music:bloom:%s";

    // ---------- 缓存统计 ----------
    public static final String CACHE_STATS = "music:cache:stats:%s";

    // ---------- 分布式 ID ----------
    public static final String ID_GEN = "music:id:%s";
}
