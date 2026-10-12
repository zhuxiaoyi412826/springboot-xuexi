package org.example.security.dto;

import lombok.Data;
import org.example.security.entity.Comment;

import java.time.LocalDateTime;

@Data
public class CommentVO {

    private Long id;
    private Long videoId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createTime;
    /** 当前用户能否删除（对象级权限） */
    private Boolean canDelete;

    public static CommentVO from(Comment c) {
        CommentVO vo = new CommentVO();
        vo.setId(c.getId());
        vo.setVideoId(c.getVideoId());
        vo.setUserId(c.getUserId());
        vo.setUsername(c.getUsername());
        vo.setContent(c.getContent());
        vo.setCreateTime(c.getCreateTime());
        return vo;
    }
}
