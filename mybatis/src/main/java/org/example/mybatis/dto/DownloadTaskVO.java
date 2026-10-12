package org.example.mybatis.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 下载任务 VO（多表：任务 + 小说）
 */
@Data
public class DownloadTaskVO implements Serializable {

    private Long id;
    private String taskNo;
    private Long novelId;
    private String status;
    private Integer totalChapter;
    private Integer doneChapter;
    private Long fileSize;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;

    /** 小说信息 */
    private String title;
    private String author;
    private String coverUrl;

    /** 下载进度百分比（前端直接用） */
    public Integer getPercent() {
        if (totalChapter == null || totalChapter <= 0) {
            return 0;
        }
        return Math.min(100, doneChapter * 100 / totalChapter);
    }
}
