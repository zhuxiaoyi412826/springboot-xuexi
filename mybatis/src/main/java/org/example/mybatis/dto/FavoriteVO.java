package org.example.mybatis.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 我的收藏 VO（多表：收藏 + 小说 + 分类）
 */
@Data
public class FavoriteVO {

    private Long id;
    private Long novelId;
    private LocalDateTime createTime;

    private String title;
    private String author;
    private String coverUrl;
    private String categoryName;
    private List<String> tags;
    private String status;
    private Long wordCount;
    private Integer chapterCount;

    /** 阅读进度（来自阅读历史，可能为空） */
    private Integer progress;
    private Integer lastChapterNo;
}
