package org.example.mybatis.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说详情 VO（多表：小说 + 分类 + 章节统计 + 我的阅读进度）
 *
 * <p>注意：NovelMapper 命名空间开启了二级缓存 {@code <cache>}，
 * 所以这个命名空间的返回结果必须实现 {@link Serializable}，否则序列化会失败。
 */
@Data
public class NovelDetailVO implements Serializable {

    private Long id;
    private String title;
    private String author;
    private Long categoryId;
    private String categoryName;
    private List<String> tags;
    private String coverUrl;
    private String description;
    private Long wordCount;
    private Integer chapterCount;
    private String status;
    private Long viewCount;
    private Integer favoriteCount;
    private Long downloadCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /** 是否已收藏 */
    private Boolean favorited;

    /** 分类对象：XML {@code <association>} 一对一嵌套映射 */
    private org.example.mybatis.entity.Category category;

    /** 我最近读到第几章（没读过为 null） */
    private Integer myChapterNo;

    /** 我的阅读进度百分比 */
    private Integer myProgress;

    /** 第一章 ID，用于「开始阅读」 */
    private Long firstChapterId;

    /** 最新章节 ID */
    private Long latestChapterId;

    private String latestChapterTitle;
}
