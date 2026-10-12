-- ============================================================
-- 在线视频浏览网站（Spring Security 学习项目）表结构
-- 幂等：可重复执行
-- ============================================================

CREATE DATABASE IF NOT EXISTS video_site DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE video_site;

-- 用户表：password 存 {bcrypt}/{argon2}/{pbkdf2} 前缀密文；digest_hash 是 HTTP Digest 用的 A1
CREATE TABLE IF NOT EXISTS sys_user (
    id                 BIGINT       NOT NULL AUTO_INCREMENT,
    username           VARCHAR(64)  NOT NULL COMMENT '登录名',
    password           VARCHAR(200) NOT NULL COMMENT '密文密码 {bcrypt}.../{argon2}...',
    digest_hash        VARCHAR(64)  NULL     COMMENT 'MD5(username:realm:password)，HTTP Digest 认证用',
    nickname           VARCHAR(64)  NULL,
    email              VARCHAR(120) NULL,
    phone              VARCHAR(32)  NULL,
    avatar             VARCHAR(500) NULL,
    enabled            TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用',
    account_non_locked TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否未锁定',
    locked_until       DATETIME     NULL     COMMENT '锁定截止时间',
    login_fail_count   INT          NOT NULL DEFAULT 0,
    last_login_time    DATETIME     NULL,
    last_login_ip      VARCHAR(64)  NULL,
    source             VARCHAR(20)  NULL     COMMENT 'local/github/ldap',
    create_time        DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 角色表（sys_user 的 roles 字段由 JPA @ElementCollection 映射到本表）
CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id   BIGINT      NOT NULL,
    role_code VARCHAR(50) NOT NULL COMMENT 'ROLE_USER / ROLE_ADMIN / ROLE_VIP',
    PRIMARY KEY (user_id, role_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 视频表
CREATE TABLE IF NOT EXISTS video (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    title        VARCHAR(200) NOT NULL,
    description  VARCHAR(1000) NULL,
    cover_url    VARCHAR(500) NULL,
    video_url    VARCHAR(500) NOT NULL,
    category     VARCHAR(50)  NULL,
    tags         VARCHAR(200) NULL,
    duration_sec INT          NULL,
    author_id    BIGINT       NOT NULL,
    author_name  VARCHAR(64)  NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PUBLIC' COMMENT 'PUBLIC/PRIVATE/REVIEW',
    view_count   BIGINT       NOT NULL DEFAULT 0,
    like_count   BIGINT       NOT NULL DEFAULT 0,
    create_time  DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_video_status (status),
    KEY idx_video_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    video_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    username   VARCHAR(64) NOT NULL,
    content    VARCHAR(500) NOT NULL,
    status     INT         NOT NULL DEFAULT 0 COMMENT '0正常 1已删除',
    create_time DATETIME   NULL,
    PRIMARY KEY (id),
    KEY idx_comment_video (video_id, status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id         BIGINT NOT NULL AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    video_id   BIGINT NOT NULL,
    create_time DATETIME NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_fav_user_video (user_id, video_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 登录日志（认证事件审计）
CREATE TABLE IF NOT EXISTS login_log (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    username    VARCHAR(64) NULL,
    ip          VARCHAR(64) NULL,
    user_agent  VARCHAR(300) NULL,
    type        VARCHAR(30) NULL COMMENT 'LOGIN_SUCCESS/LOGIN_FAILURE/LOGOUT/KICK',
    success     TINYINT(1)  NOT NULL DEFAULT 1,
    message     VARCHAR(200) NULL,
    create_time DATETIME    NULL,
    PRIMARY KEY (id),
    KEY idx_log_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- 记住我（Remember-Me 持久化令牌，JdbcTokenRepositoryImpl 使用）
CREATE TABLE IF NOT EXISTS persistent_logins (
    username  VARCHAR(64)  NOT NULL,
    series    VARCHAR(64)  NOT NULL,
    token     VARCHAR(64)  NOT NULL,
    last_used TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (series)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
