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
 * 视频表。status：PUBLIC（公开）/ PRIVATE（作者私有）/ REVIEW（待审核）。
 */
@Data
@Entity
@Table(name = "video")
public class Video implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 1000)
    private String description;

    /** 封面地址（可为空，前端用占位色块） */
    @Column(name = "cover_url", length = 500)
    private String coverUrl;

    /** 播放地址（外部 URL 或 /media/xxx.mp4） */
    @Column(name = "video_url", nullable = false, length = 500)
    private String videoUrl;

    @Column(length = 50)
    private String category;

    @Column(length = 200)
    private String tags;

    /** 时长（秒） */
    @Column(name = "duration_sec")
    private Integer durationSec;

    /** 作者（上传者） */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "author_name", length = 64)
    private String authorName;

    @Column(nullable = false, length = 20)
    private String status = "PUBLIC";

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    @Column(name = "like_count", nullable = false)
    private Long likeCount = 0L;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
