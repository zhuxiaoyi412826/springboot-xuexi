package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 下载任务
 *
 * <p>状态流转：QUEUED（排队） -> DOWNLOADING（下载中） -> { PAUSED（暂停）/ COMPLETED（完成） / CANCELLED（取消）/ FAILED（失败） }
 * PAUSED 可以再次 DOWNLOADING，形成可恢复的任务。
 */
@Data
public class DownloadTask implements Serializable {

    private Long id;
    private String taskNo;
    private Long userId;
    private Long novelId;
    private String status;
    private Integer totalChapter;
    private Integer doneChapter;
    private Long fileSize;
    private String errorMsg;
    private LocalDateTime createTime;
    private LocalDateTime startTime;
    private LocalDateTime finishTime;
    private LocalDateTime updateTime;
}
