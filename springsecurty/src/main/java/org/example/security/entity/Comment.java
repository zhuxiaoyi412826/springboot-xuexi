package org.example.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论表。status：0 正常 / 1 已删除（软删除）。
 * 对象级权限演示：只有评论作者本人或管理员才能删除。
 */
@Data
@Entity
@Table(name = "comment")
public class Comment implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private Integer status = 0;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
