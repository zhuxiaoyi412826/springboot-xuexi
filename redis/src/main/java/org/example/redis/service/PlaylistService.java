package org.example.redis.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.example.redis.cache.MultiLevelCacheService;
import org.example.redis.common.BizException;
import org.example.redis.common.PageResult;
import org.example.redis.entity.Playlist;
import org.example.redis.entity.PlaylistSong;
import org.example.redis.entity.Song;
import org.example.redis.mapper.PlaylistMapper;
import org.example.redis.mapper.PlaylistSongMapper;
import org.example.redis.mapper.SongMapper;
import org.example.redis.redis.RedisKeys;
import org.example.redis.redis.RedisService;
import org.example.redis.vo.PlaylistVO;
import org.example.redis.vo.SongVO;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌单业务。
 *
 * <p>Redis 知识点：
 * <ul>
 *   <li>歌单歌曲 id 列表用 List 缓存（LRANGE 一次取完，不用挨个查库）</li>
 *   <li>歌单计数用 Hash 存（HMSET / HINCRBY）</li>
 *   <li>用户新建歌单先进「待审核队列」（List + LPUSH/RPOP）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private static final String CACHE_PLAYLIST = "playlist";

    private final PlaylistMapper playlistMapper;
    private final PlaylistSongMapper playlistSongMapper;
    private final SongMapper songMapper;
    private final MultiLevelCacheService cache;
    private final RedisService redis;
    private final SongService songService;

    /** 公开歌单列表 */
    public PageResult<PlaylistVO> page(int page, int size) {
        LambdaQueryWrapper<Playlist> qw = new LambdaQueryWrapper<Playlist>()
                .eq(Playlist::getStatus, 1)
                .orderByDesc(Playlist::getPlayCount);
        IPage<Playlist> p = playlistMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, this::toVo);
    }

    /** 歌单详情：先取 Redis List 里的歌曲 id，再批量查歌曲 */
    public PlaylistVO detail(Long id) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BizException("歌单不存在");
        }
        PlaylistVO vo = toVo(playlist);
        String listKey = String.format(RedisKeys.PLAYLIST_SONGS, id);
        List<String> songIds = redis.lRange(listKey, 0, -1);
        List<SongVO> songs = new ArrayList<>();
        if (songIds.isEmpty()) {
            // 回源：查关系表并回填 List
            List<PlaylistSong> refs = playlistSongMapper.selectList(
                    new LambdaQueryWrapper<PlaylistSong>().eq(PlaylistSong::getPlaylistId, id)
                            .orderByAsc(PlaylistSong::getSortNo));
            for (PlaylistSong ref : refs) {
                redis.lPush(listKey + ":tmp", String.valueOf(ref.getSongId()));
                songs.add(songService.detail(ref.getSongId()));
            }
            // 用 pipeline 一次性写入（缓存回填的经典写法）
            redis.pipeline(t -> {
                List<String> ids = redis.lRange(listKey + ":tmp", 0, -1);
                for (String sid : ids) {
                    redis.lPush(listKey, sid);
                }
                redis.expire(listKey, Duration.ofHours(2));
                redis.delete(listKey + ":tmp");
            });
        } else {
            for (String sid : songIds) {
                SongVO s = songService.detail(Long.valueOf(sid));
                if (s != null) {
                    songs.add(s);
                }
            }
        }
        vo.setSongs(songs);
        return vo;
    }

    /** 新建歌单：写库后进入待审核队列（List） */
    public PlaylistVO create(String name, String description, String coverUrl) {
        Long uid = songService.currentUserIdOrNull();
        if (uid == null) {
            throw new BizException(401, "请先登录");
        }
        Playlist playlist = new Playlist();
        playlist.setName(name);
        playlist.setDescription(description);
        playlist.setCoverUrl(coverUrl);
        playlist.setUserId(uid);
        playlist.setStatus(2);
        playlist.setPlayCount(0L);
        playlistMapper.insert(playlist);
        // 待审核队列（LPUSH，管理端 RPOP 处理）
        redis.lPush(RedisKeys.AUDIT_QUEUE, String.valueOf(playlist.getId()));
        return toVo(playlist);
    }

    /** 往歌单加歌 */
    public void addSong(Long playlistId, Long songId) {
        Playlist playlist = playlistMapper.selectById(playlistId);
        if (playlist == null) {
            throw new BizException("歌单不存在");
        }
        Long count = playlistSongMapper.selectCount(new LambdaQueryWrapper<PlaylistSong>()
                .eq(PlaylistSong::getPlaylistId, playlistId).eq(PlaylistSong::getSongId, songId));
        if (count > 0) {
            return;
        }
        PlaylistSong ref = new PlaylistSong();
        ref.setPlaylistId(playlistId);
        ref.setSongId(songId);
        ref.setSortNo(0);
        playlistSongMapper.insert(ref);
        // 缓存里的歌曲 List 同步追加
        String listKey = String.format(RedisKeys.PLAYLIST_SONGS, playlistId);
        if (redis.lLen(listKey) > 0) {
            redis.lPush(listKey, String.valueOf(songId));
        }
    }

    /** 审核（管理端）：从队列里取出并改状态 */
    public void audit(Long id, int status) {
        Playlist playlist = playlistMapper.selectById(id);
        if (playlist == null) {
            throw new BizException("歌单不存在");
        }
        playlist.setStatus(status);
        playlistMapper.updateById(playlist);
        cache.evict(CACHE_PLAYLIST, String.valueOf(id));
    }

    /** 待审核队列内容（不弹出，管理端查看用） */
    public List<Long> auditQueue() {
        return redis.lRange(RedisKeys.AUDIT_QUEUE, 0, -1).stream().map(Long::valueOf).toList();
    }

    public PageResult<PlaylistVO> adminPage(int page, int size) {
        LambdaQueryWrapper<Playlist> qw = new LambdaQueryWrapper<Playlist>().orderByDesc(Playlist::getId);
        IPage<Playlist> p = playlistMapper.selectPage(new Page<>(page, size), qw);
        return PageResult.of(p, this::toVo);
    }

    private PlaylistVO toVo(Playlist playlist) {
        PlaylistVO vo = PlaylistVO.from(playlist);
        var user = songService.currentUserIdOrNull();
        if (user != null && playlist.getUserId().equals(user)) {
            vo.setNickname("我");
        }
        return vo;
    }

    /** 播放歌单：计数用 Hash 自增 */
    public long incrPlayCount(Long id) {
        redis.hIncrBy(String.format(RedisKeys.PLAYLIST, id), "playCount", 1);
        return redis.lLen(String.format(RedisKeys.PLAYLIST_SONGS, id));
    }

    public List<Song> songsOf(Long playlistId) {
        List<String> ids = redis.lRange(String.format(RedisKeys.PLAYLIST_SONGS, playlistId), 0, -1);
        return ids.stream().map(id -> songMapper.selectById(Long.valueOf(id)))
                .filter(java.util.Objects::nonNull).toList();
    }
}
