package org.example.redis.vo;

import lombok.Data;
import org.example.redis.entity.Song;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 歌曲视图对象（带“当前用户是否点赞/收藏”的标记）。 */
@Data
public class SongVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String title;
    private String singer;
    private String album;
    private String category;
    private Integer duration;
    private String coverUrl;
    private String audioUrl;
    private String lyric;
    private Integer status;
    private Long playCount;
    private Long likeCount;
    private LocalDateTime createTime;
    /** 当前登录用户是否点赞过（Set 判断，O(1)） */
    private boolean liked;
    /** 当前登录用户是否收藏过 */
    private boolean favorited;

    public static SongVO from(Song song) {
        SongVO vo = new SongVO();
        vo.setId(song.getId());
        vo.setTitle(song.getTitle());
        vo.setSinger(song.getSinger());
        vo.setAlbum(song.getAlbum());
        vo.setCategory(song.getCategory());
        vo.setDuration(song.getDuration());
        vo.setCoverUrl(song.getCoverUrl());
        vo.setAudioUrl(song.getAudioUrl());
        vo.setLyric(song.getLyric());
        vo.setStatus(song.getStatus());
        vo.setPlayCount(song.getPlayCount());
        vo.setLikeCount(song.getLikeCount());
        vo.setCreateTime(song.getCreateTime());
        return vo;
    }
}
