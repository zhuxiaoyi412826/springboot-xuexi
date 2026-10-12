package org.example.redis.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.redis.cache.BloomFilterService;
import org.example.redis.cache.CacheProperties;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.common.BizException;
import org.example.redis.common.PageResult;
import org.example.redis.entity.Favorite;
import org.example.redis.entity.Song;
import org.example.redis.mapper.FavoriteMapper;
import org.example.redis.mapper.SongMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.SongVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲业务（缓存与计数用法最集中的地方）。
 *
 * <p>Redis 知识点：
 * <ul>
 *   <li>列表/详情走 L1/L2/L3 三级缓存，详情是热点 key → 逻辑过期 + 异步重建</li>
 *   <li>播放量用 String INCR 计数（高频写不直接打数据库），定时异步落库</li>
 *   <li>热歌榜用 ZSet（ZINCRBY / ZREVRANGE）</li>
 *   <li>点赞用 Set（SADD / SISMEMBER，天然去重）</li>
 *   <li>最近播放用 List（LPUSH + LTRIM 只保留 20 条）</li>
 *   <li>播放流水写 Stream，消费者异步落库（削峰）</li>
 *   <li>日活用 Bitmap，UV 用 HyperLogLog</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {

    private static final String CACHE_SONG = "song";
    private static final String CACHE_SONG_LIST = "songList";
    private static final String BLOOM_SONG = "song";
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final SongMapper songMapper;
    private final FavoriteMapper favoriteMapper;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final BloomFilterService bloom;
    private final CacheProperties cacheProperties;
    private final ObjectMapper mapper;

    // ==================== 查询 ====================

    public PageResult<SongVO> page(int page, int size, String keyword, String category) {
        String key = "p" + page + "_s" + size + "_k" + (keyword == null ? "" : keyword) + "_c" + (category == null ? "" : category);
        JavaType type = mapper.getTypeFactory().constructParametricType(PageResult.class, SongVO.class);
        PageResult<SongVO> result = cache.get(CACHE_SONG_LIST, key, type, Duration.ofMinutes(5),
                () -> queryPage(page, size, keyword, category));
        return fillUserFlags(result);
    }

    private PageResult<SongVO> queryPage(int page, int size, String keyword, String category) {
        LambdaQueryWrapper<Song> qw = new LambdaQueryWrapper<>();
        qw.eq(Song::getStatus, 1);
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(Song::getTitle, keyword).or().like(Song::getSinger, keyword));
        }
        if (category != null && !category.isBlank()) {
            qw.eq(Song::getCategory, category);
        }
        qw.orderByDesc(Song::getPlayCount);
        IPage<Song> p = songMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, SongVO::from);
    }

    /** 详情：热点 key，用逻辑过期 + 互斥锁重建 */
    public SongVO detail(Long id) {
        if (cacheProperties.isBloomEnabled() && bloom.isReady(BLOOM_SONG) && !bloom.mightContain(BLOOM_SONG, String.valueOf(id))) {
            log.debug("布隆过滤器判定歌曲 {} 不存在，直接返回", id);
            return null;
        }
        SongVO vo = cache.getHot(CACHE_SONG, String.valueOf(id), SongVO.class, () -> {
            Song song = songMapper.selectById(id);
            return song == null ? null : SongVO.from(song);
        });
        if (vo == null) {
            return null;
        }
        Long uid = currentUserIdOrNull();
        if (uid != null) {
            vo.setLiked(redis.sIsMember(String.format(RedisKeys.LIKE_USERS, id), String.valueOf(uid)));
            vo.setFavorited(redis.sIsMember(String.format(RedisKeys.FAV_SONGS, uid), String.valueOf(id)));
        }
        return vo;
    }

    /** 热歌榜（ZSet） */
    public List<SongVO> hot(int limit) {
        List<String> ids = redis.zRevRange(RedisKeys.RANK_HOT, 0, limit - 1);
        List<SongVO> list = new ArrayList<>();
        for (String id : ids) {
            SongVO vo = detail(Long.valueOf(id));
            if (vo != null) {
                list.add(vo);
            }
        }
        if (!list.isEmpty()) {
            return list;
        }
        // 榜单还没建起来（首次启动）：用数据库播放量兜底并写进 ZSet
        LambdaQueryWrapper<Song> qw = new LambdaQueryWrapper<Song>()
                .eq(Song::getStatus, 1).orderByDesc(Song::getPlayCount).last("limit " + limit);
        List<Song> songs = songMapper.selectList(qw);
        for (Song s : songs) {
            redis.zAdd(RedisKeys.RANK_HOT, String.valueOf(s.getId()), s.getPlayCount() == null ? 0 : s.getPlayCount());
            list.add(SongVO.from(s));
        }
        return list;
    }

    // ==================== 播放 ====================

    /** 播放一次：计数 + 榜单 + 最近播放 + Stream 流水 + 日活/UV */
    public long play(Long songId) {
        Song song = songMapper.selectById(songId);
        if (song == null) {
            throw new BizException("歌曲不存在");
        }

        // 1) 播放量计数（String INCR，避免高频 UPDATE 数据库）
        long count = redis.incr(String.format(RedisKeys.CNT_PLAY, songId));
        // 2) 热歌榜加分（ZSet）
        redis.zIncrBy(RedisKeys.RANK_HOT, String.valueOf(songId), 1);
        // 3) 播放流水写 Stream，由消费者异步落库
        java.util.Map<String, String> event = new java.util.LinkedHashMap<>();
        event.put("songId", String.valueOf(songId));
        event.put("userId", String.valueOf(currentUserIdOrNull()));
        event.put("ts", String.valueOf(System.currentTimeMillis()));
        try {
            redis.xAdd(RedisKeys.STREAM_PLAY, event);
        } catch (Exception e) {
            log.warn("写入播放流失败：{}", e.getMessage());
        }

        Long uid = currentUserIdOrNull();
        String day = LocalDate.now().format(YMD);
        if (uid != null) {
            // 4) 最近播放（List，只保留 20 条）
            redis.lPushAndTrim(String.format(RedisKeys.RECENT_PLAY, uid), String.valueOf(songId), 20);
            // 5) 日活（Bitmap）：offset 用 userId
            redis.setBit(String.format(RedisKeys.ACTIVE, day), uid, true);
            // 6) UV（HyperLogLog）
            redis.pfAdd(String.format(RedisKeys.UV, day), String.valueOf(uid));
        }
        return count;
    }

    /** 最近播放 */
    public List<SongVO> recent(int limit) {
        Long uid = currentUserIdOrNull();
        if (uid == null) {
            return List.of();
        }
        List<String> ids = redis.lRange(String.format(RedisKeys.RECENT_PLAY, uid), 0, limit - 1L);
        List<SongVO> list = new ArrayList<>();
        for (String id : ids) {
            SongVO vo = detail(Long.valueOf(id));
            if (vo != null) {
                list.add(vo);
            }
        }
        return list;
    }

    // ==================== 点赞 / 收藏 ====================

    /** 点赞（取消点赞）：Set 天然去重 */
    public boolean like(Long songId) {
        Long uid = requireLogin();
        String key = String.format(RedisKeys.LIKE_USERS, songId);
        if (redis.sIsMember(key, String.valueOf(uid))) {
            redis.sRem(key, String.valueOf(uid));
            redis.incrBy(String.format(RedisKeys.CNT_LIKE, songId), -1);
            return false;
        }
        redis.sAdd(key, String.valueOf(uid));
        redis.incrBy(String.format(RedisKeys.CNT_LIKE, songId), 1);
        return true;
    }

    public long likeCount(Long songId) {
        Long n = redis.sCard(String.format(RedisKeys.LIKE_USERS, songId));
        return n == null ? 0L : n;
    }

    /** 收藏（取消收藏）：Set 判断 + 数据库落库 */
    public boolean favorite(Long songId) {
        Long uid = requireLogin();
        String favKey = String.format(RedisKeys.FAV_SONGS, uid);
        if (redis.sIsMember(favKey, String.valueOf(songId))) {
            redis.sRem(favKey, String.valueOf(songId));
            favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                    .eq(Favorite::getUserId, uid).eq(Favorite::getSongId, songId));
            return false;
        }
        redis.sAdd(favKey, String.valueOf(songId));
        redis.sAdd(String.format(RedisKeys.FAV_USERS, songId), String.valueOf(uid));
        Favorite fav = new Favorite();
        fav.setUserId(uid);
        fav.setSongId(songId);
        favoriteMapper.insert(fav);
        return true;
    }

    public List<SongVO> favorites() {
        Long uid = requireLogin();
        var ids = redis.sMembers(String.format(RedisKeys.FAV_SONGS, uid));
        List<SongVO> list = new ArrayList<>();
        for (String id : ids) {
            SongVO vo = detail(Long.valueOf(id));
            if (vo != null) {
                list.add(vo);
            }
        }
        return list;
    }

    /** 共同收藏：Set 交集（SINTER 的经典场景） */
    public List<Long> commonFavorite(Long otherUserId) {
        Long uid = requireLogin();
        var inter = redis.sInter(String.format(RedisKeys.FAV_SONGS, uid),
                String.format(RedisKeys.FAV_SONGS, otherUserId));
        return inter.stream().map(Long::valueOf).toList();
    }

    // ==================== 管理端：增删改（改库后必须删缓存） ====================

    public Song add(Song song) {
        song.setPlayCount(0L);
        song.setLikeCount(0L);
        if (song.getStatus() == null) {
            song.setStatus(1);
        }
        songMapper.insert(song);
        // 新歌进布隆过滤器，否则详情会被布隆拦截
        bloom.add(BLOOM_SONG, String.valueOf(song.getId()));
        bloom.markReady(BLOOM_SONG);
        cache.clear(CACHE_SONG_LIST);
        return song;
    }

    public Song update(Song song) {
        Song exist = songMapper.selectById(song.getId());
        if (exist == null) {
            throw new BizException("歌曲不存在");
        }
        songMapper.updateById(song);
        // 先删缓存，下次读取自动回源（Cache-Aside 模式）
        cache.evict(CACHE_SONG, String.valueOf(song.getId()));
        cache.clear(CACHE_SONG_LIST);
        return song;
    }

    public void delete(Long id) {
        songMapper.deleteById(id);
        cache.evict(CACHE_SONG, String.valueOf(id));
        cache.clear(CACHE_SONG_LIST);
        redis.zRem(RedisKeys.RANK_HOT, String.valueOf(id));
    }

    /** 上下架：下架后要立刻从缓存与榜单里摘掉 */
    public void setStatus(Long id, int status) {
        Song song = songMapper.selectById(id);
        if (song == null) {
            throw new BizException("歌曲不存在");
        }
        song.setStatus(status);
        songMapper.updateById(song);
        cache.evict(CACHE_SONG, String.valueOf(id));
        cache.clear(CACHE_SONG_LIST);
        if (status == 0) {
            redis.zRem(RedisKeys.RANK_HOT, String.valueOf(id));
        }
    }

    /** 管理端列表（含下架） */
    public PageResult<SongVO> adminPage(int page, int size, String keyword) {
        LambdaQueryWrapper<Song> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.like(Song::getTitle, keyword).or().like(Song::getSinger, keyword);
        }
        qw.orderByDesc(Song::getId);
        IPage<Song> p = songMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, SongVO::from);
    }

    // ==================== 工具 ====================

    private PageResult<SongVO> fillUserFlags(PageResult<SongVO> page) {
        Long uid = currentUserIdOrNull();
        if (uid == null || page.getList() == null) {
            return page;
        }
        for (SongVO vo : page.getList()) {
            vo.setLiked(redis.sIsMember(String.format(RedisKeys.LIKE_USERS, vo.getId()), String.valueOf(uid)));
            vo.setFavorited(redis.sIsMember(String.format(RedisKeys.FAV_SONGS, uid), String.valueOf(vo.getId())));
        }
        return page;
    }

    public Long currentUserIdOrNull() {
        try {
            if (!StpUtil.isLogin()) {
                return null;
            }
            return Long.parseLong(StpUtil.getLoginId().toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Long requireLogin() {
        Long uid = currentUserIdOrNull();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        return uid;
    }
}
