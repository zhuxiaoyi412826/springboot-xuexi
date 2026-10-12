package org.example.redis.vo;

import lombok.Data;
import org.example.redis.entity.Playlist;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/** 歌单视图对象（含歌曲列表）。 */
@Data
public class PlaylistVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Long userId;
    private String nickname;
    private Integer status;
    private Long playCount;
    private LocalDateTime createTime;
    private List<SongVO> songs = List.of();

    public static PlaylistVO from(Playlist playlist) {
        PlaylistVO vo = new PlaylistVO();
        vo.setId(playlist.getId());
        vo.setName(playlist.getName());
        vo.setDescription(playlist.getDescription());
        vo.setCoverUrl(playlist.getCoverUrl());
        vo.setUserId(playlist.getUserId());
        vo.setStatus(playlist.getStatus());
        vo.setPlayCount(playlist.getPlayCount());
        vo.setCreateTime(playlist.getCreateTime());
        return vo;
    }
}
