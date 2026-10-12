-- ============================================================
-- 在线听歌网站 表结构（music_db）
-- 手动执行：mysql -uroot -p412826 < db/schema.sql
-- ============================================================
CREATE DATABASE IF NOT EXISTS music_db DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_general_ci;
USE music_db;

-- 用户表（user / admin 两种角色，共用登录页）
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(64)  NOT NULL COMMENT '登录名',
    password     VARCHAR(128) NOT NULL COMMENT '密码（BCrypt 密文）',
    nickname     VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar       VARCHAR(255) DEFAULT NULL COMMENT '头像',
    role         VARCHAR(32)  NOT NULL DEFAULT 'user' COMMENT '角色：user/admin',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用',
    sign_days    INT          NOT NULL DEFAULT 0 COMMENT '累计签到天数',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 歌曲表
CREATE TABLE IF NOT EXISTS song (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    title       VARCHAR(128) NOT NULL COMMENT '歌曲名',
    singer      VARCHAR(64)  NOT NULL COMMENT '歌手',
    album       VARCHAR(128) DEFAULT NULL COMMENT '专辑',
    category    VARCHAR(32)  DEFAULT '流行' COMMENT '分类：流行/民谣/电子/古典/摇滚',
    duration    INT          NOT NULL DEFAULT 0 COMMENT '时长（秒）',
    cover_url   VARCHAR(255) DEFAULT NULL COMMENT '封面',
    audio_url   VARCHAR(255) NOT NULL COMMENT '音频地址',
    lyric       TEXT         COMMENT '歌词（每行一句，用 | 分隔示例）',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1上架 0下架',
    play_count  BIGINT       NOT NULL DEFAULT 0 COMMENT '播放量',
    like_count  BIGINT       NOT NULL DEFAULT 0 COMMENT '点赞数',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_status (status),
    KEY idx_play_count (play_count),
    KEY idx_category (category)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌曲表';

-- 歌单表
CREATE TABLE IF NOT EXISTS playlist (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(128) NOT NULL COMMENT '歌单名',
    description VARCHAR(255) DEFAULT NULL,
    cover_url   VARCHAR(255) DEFAULT NULL,
    user_id     BIGINT       NOT NULL COMMENT '创建人',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1公开 0私有 2待审核',
    play_count  BIGINT       NOT NULL DEFAULT 0,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌单表';

-- 歌单-歌曲关系表
CREATE TABLE IF NOT EXISTS playlist_song (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    playlist_id BIGINT   NOT NULL,
    song_id     BIGINT   NOT NULL,
    sort_no     INT      NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_playlist_song (playlist_id, song_id),
    KEY idx_song (song_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌单歌曲关系';

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    song_id     BIGINT       NOT NULL,
    user_id     BIGINT       NOT NULL,
    content     VARCHAR(512) NOT NULL,
    like_count  BIGINT       NOT NULL DEFAULT 0,
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '1正常 0屏蔽',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_song (song_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '歌曲评论';

-- 收藏表（用户收藏歌曲）
CREATE TABLE IF NOT EXISTS favorite (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   NOT NULL,
    song_id     BIGINT   NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     TINYINT  NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_song (user_id, song_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '收藏';

-- 播放流水（Redis Stream 异步消费后落库）
CREATE TABLE IF NOT EXISTS play_record (
    id          BIGINT   NOT NULL AUTO_INCREMENT,
    user_id     BIGINT   DEFAULT NULL,
    song_id     BIGINT   NOT NULL,
    source      VARCHAR(32) DEFAULT 'web' COMMENT '来源：web/app/stream',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_song (song_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '播放流水';
