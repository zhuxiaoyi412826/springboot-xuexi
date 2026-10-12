package org.example.mybatis.dto;

import lombok.Data;

/**
 * 下载任务查询条件
 */
@Data
public class TaskQuery {

    private Long userId;

    /** QUEUED / DOWNLOADING / PAUSED / COMPLETED / CANCELLED / FAILED */
    private String status;

    /** 多个状态（演示 foreach） */
    private java.util.List<String> statuses;

    private String keyword;

    private String orderBy;
}
