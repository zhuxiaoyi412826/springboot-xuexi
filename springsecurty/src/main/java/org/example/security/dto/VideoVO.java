package org.example.security.dto;

import lombok.Data;
import org.example.security.entity.Video;

import java.time.LocalDateTime;

/** 视频列表/详情 VO（含「当前用户能否操作」的对象级权限标记） */
@Data
public class VideoVO {

    private Long id;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private String category;
    private String tags;
    private Integer durationSec;
    private Long authorId;
    private String authorName;
    private String status;
    private Long viewCount;
    private Long likeCount;
    private LocalDateTime createTime;

    /** 当前用户是否已收藏 */
    private Boolean favorited;
    /** 当前用户是否能编辑/删除（对象级权限结果） */
    private Boolean canEdit;

    public static VideoVO from(Video v) {
        VideoVO vo = new VideoVO();
        vo.setId(v.getId());
        vo.setTitle(v.getTitle());
        vo.setDescription(v.getDescription());
        vo.setCoverUrl(v.getCoverUrl());
        vo.setVideoUrl(v.getVideoUrl());
        vo.setCategory(v.getCategory());
        vo.setTags(v.getTags());
        vo.setDurationSec(v.getDurationSec());
        vo.setAuthorId(v.getAuthorId());
        vo.setAuthorName(v.getAuthorName());
        vo.setStatus(v.getStatus());
        vo.setViewCount(v.getViewCount());
        vo.setLikeCount(v.getLikeCount());
        vo.setCreateTime(v.getCreateTime());
        return vo;
    }
}
