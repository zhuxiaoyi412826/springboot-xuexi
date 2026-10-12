package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 阅读历史（我的书架 + 阅读进度）
 */
@Data
public class ReadingHistory implements Serializable {

    private Long id;
    private Long userId;
    private Long novelId;
    private Long chapterId;
    private Integer chapterNo;
    private Integer progress;
    private LocalDateTime lastReadTime;
}
