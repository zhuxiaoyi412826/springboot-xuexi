package org.example.mybatis.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说列表 VO（多表关联：小说 + 分类名）
 *
 * <p>同 NovelDetailVO：因为 NovelMapper 开了二级缓存，必须实现 {@link Serializable}。
 */
@Data
public class NovelVO implements Serializable {

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

    /** 当前用户是否收藏过 */
    private Boolean favorited;

    /** 最新章节标题（子查询得出） */
    private String latestChapter;
}
