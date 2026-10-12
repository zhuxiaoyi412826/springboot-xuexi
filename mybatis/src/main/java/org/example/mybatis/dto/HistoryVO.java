package org.example.mybatis.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 书架 / 阅读历史 VO（多表：阅读历史 + 小说 + 最新章节标题）
 */
@Data
public class HistoryVO implements Serializable {

    private Long id;
    private Long novelId;
    private Long chapterId;
    private Integer chapterNo;
    private Integer progress;
    private LocalDateTime lastReadTime;

    private String title;
    private String author;
    private String coverUrl;
    private String categoryName;
    private List<String> tags;
    private Integer totalChapter;

    /** 最后阅读的章节标题 */
    private String chapterTitle;

    /** 下一章序号（读到最后一章时为 null） */
    private Integer nextChapterNo;
}
