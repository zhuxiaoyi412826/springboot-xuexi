package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志
 */
@Data
public class SysLog implements Serializable {

    private Long id;
    private Long userId;
    private String username;
    private String module;
    private String operation;
    private String method;
    private Integer costMs;
    private LocalDateTime createTime;
}
