package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 小说
 *
 * <p>{@code tags} 字段演示自定义 {@code TypeHandler}：库里是 JSON 数组文本，Java 里是 List。
 */
@Data
public class Novel implements Serializable {

    private Long id;
    private String title;
    private String author;
    private Long categoryId;
    private List<String> tags;
    private String coverUrl;
    private String description;
    private Long wordCount;
    private Integer chapterCount;

    /** SERIALIZING 连载 / FINISHED 完结 */
    private String status;

    private Long viewCount;
    private Integer favoriteCount;
    private Long downloadCount;
    private String filePath;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;
}
