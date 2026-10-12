package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小说章节
 */
@Data
public class Chapter implements Serializable {

    private Long id;
    private Long novelId;
    private Integer chapterNo;
    private String title;
    private String content;
    private Integer wordCount;
    private LocalDateTime createTime;
}
