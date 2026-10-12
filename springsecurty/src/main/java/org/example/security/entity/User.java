package org.example.security.entity;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户表。
 *
 * <p>password 存储的是 {@code {bcrypt}$2a$...} / {@code {argon2}$argon2id$...} 这类
 * 带算法前缀的密文，由 DelegatingPasswordEncoder 按前缀选择解码器，绝不明文存储。</p>
 */
@Data
@Entity
@Table(name = "sys_user")
public class User implements java.io.Serializable {

    /** Spring Session（Redis）默认用 JDK 序列化保存会话属性，放进会话的对象必须可序列化 */
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /** 密文密码：{bcrypt}... / {argon2}... / {pbkdf2}... */
    @Column(nullable = false, length = 200)
    private String password;

    /**
     * HTTP Digest 认证专用：A1 = MD5(username:realm:password)。
     * Digest 无法校验 BCrypt 密文，只能比对 A1，所以注册/改密时额外算一份。
     */
    @Column(name = "digest_hash", length = 64)
    private String digestHash;

    private String nickname;

    private String email;

    private String phone;

    /** 头像（这里只存 URL） */
    private String avatar;

    /** 是否启用（禁用后无法登录） */
    @Column(nullable = false)
    private Boolean enabled = true;

    /** 是否未锁定（登录失败过多会被锁定） */
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    /** 锁定截止时间 */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "login_fail_count", nullable = false)
    private Integer loginFailCount = 0;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(name = "last_login_ip", length = 64)
    private String lastLoginIp;

    /** 注册来源：local / github / ldap */
    @Column(name = "source", length = 20)
    private String source = "local";

    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 角色（ROLE_USER / ROLE_ADMIN / ROLE_VIP） */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sys_user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role_code")
    private Set<String> roles = new HashSet<>();
}
