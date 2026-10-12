package org.example.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志（由认证事件监听器写入：成功 / 失败 / 退出 / 被踢下线）。
 */
@Data
@Entity
@Table(name = "login_log")
public class LoginLog implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64)
    private String username;

    @Column(length = 64)
    private String ip;

    @Column(name = "user_agent", length = 300)
    private String userAgent;

    /** 事件类型：LOGIN_SUCCESS / LOGIN_FAILURE / LOGOUT / KICK */
    @Column(length = 30)
    private String type;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(length = 200)
    private String message;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}
