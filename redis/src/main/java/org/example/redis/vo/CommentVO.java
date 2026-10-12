package org.example.redis.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 评论视图对象（带评论人昵称）。 */
@Data
public class CommentVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long songId;
    private Long userId;
    private String nickname;
    private String avatar;
    private String content;
    private Long likeCount;
    private Integer status;
    private LocalDateTime createTime;
}
