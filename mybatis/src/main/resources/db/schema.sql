-- ============================================================
-- 小说阅读器 - 建表脚本
-- 全部使用 CREATE TABLE IF NOT EXISTS，重复启动不会报错、不会重建
-- ============================================================

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL COMMENT '用户名',
    password     VARCHAR(100) NOT NULL COMMENT '密码',
    nickname     VARCHAR(50)  COMMENT '昵称',
    avatar       VARCHAR(500) COMMENT '头像URL',
    role         VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'ADMIN 管理员 / USER 普通用户',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '1 正常 / 0 禁用',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 分类表
CREATE TABLE IF NOT EXISTS category (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    name         VARCHAR(50)  NOT NULL COMMENT '分类名',
    description  VARCHAR(255) COMMENT '描述',
    sort_num     INT          NOT NULL DEFAULT 0 COMMENT '排序号',
    create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0,
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说分类';

-- 3. 小说表
CREATE TABLE IF NOT EXISTS novel (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    title          VARCHAR(200) NOT NULL COMMENT '书名',
    author         VARCHAR(100) COMMENT '作者',
    category_id    BIGINT COMMENT '分类ID',
    tags           VARCHAR(500) COMMENT '标签：JSON 数组字符串（演示 TypeHandler）',
    cover_url      VARCHAR(500) COMMENT '封面URL',
    description    TEXT COMMENT '简介',
    word_count     BIGINT NOT NULL DEFAULT 0 COMMENT '总字数',
    chapter_count  INT    NOT NULL DEFAULT 0 COMMENT '章节数',
    status         VARCHAR(20) NOT NULL DEFAULT 'SERIALIZING' COMMENT 'SERIALIZING 连载 / FINISHED 完结',
    view_count     BIGINT NOT NULL DEFAULT 0 COMMENT '浏览量',
    favorite_count INT    NOT NULL DEFAULT 0 COMMENT '收藏数',
    download_count BIGINT NOT NULL DEFAULT 0 COMMENT '下载数',
    file_path      VARCHAR(500) COMMENT '上传的原文件路径',
    create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted        TINYINT NOT NULL DEFAULT 0,
    KEY idx_category (category_id),
    KEY idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小说表';

-- 4. 章节表
CREATE TABLE IF NOT EXISTS chapter (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id    BIGINT  NOT NULL COMMENT '所属小说',
    chapter_no  INT     NOT NULL COMMENT '章节序号',
    title       VARCHAR(300) NOT NULL COMMENT '章节标题',
    content     MEDIUMTEXT COMMENT '正文内容',
    word_count  INT NOT NULL DEFAULT 0 COMMENT '本章字数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_novel_no (novel_id, chapter_no),
    KEY idx_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='章节表';

-- 5. 收藏表（用户 - 小说 多对多）
CREATE TABLE IF NOT EXISTS favorite (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    novel_id    BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_novel (user_id, novel_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏';

-- 6. 阅读历史（书架进度）
CREATE TABLE IF NOT EXISTS reading_history (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id        BIGINT NOT NULL,
    novel_id       BIGINT NOT NULL,
    chapter_id     BIGINT COMMENT '最近阅读的章节ID',
    chapter_no     INT NOT NULL DEFAULT 1 COMMENT '最近阅读的章节序号',
    progress       INT NOT NULL DEFAULT 0 COMMENT '阅读进度百分比',
    last_read_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_novel (user_id, novel_id),
    KEY idx_user_time (user_id, last_read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读历史/书架';

-- 7. 下载任务表（下载 / 暂停 / 继续 / 停止）
CREATE TABLE IF NOT EXISTS download_task (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no       VARCHAR(64) NOT NULL COMMENT '任务编号',
    user_id       BIGINT NOT NULL,
    novel_id      BIGINT NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'QUEUED'
                  COMMENT 'QUEUED 排队 / DOWNLOADING 下载中 / PAUSED 已暂停 / COMPLETED 已完成 / CANCELLED 已取消 / FAILED 失败',
    total_chapter INT NOT NULL DEFAULT 0 COMMENT '总章节数',
    done_chapter  INT NOT NULL DEFAULT 0 COMMENT '已完成章节数',
    file_size     BIGINT NOT NULL DEFAULT 0 COMMENT '已下载字节数',
    error_msg     VARCHAR(500) COMMENT '失败原因',
    create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
    start_time    DATETIME COMMENT '开始时间',
    finish_time   DATETIME COMMENT '结束时间',
    update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_task_no (task_no),
    KEY idx_user (user_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下载任务';

-- 8. 操作日志表
CREATE TABLE IF NOT EXISTS sys_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT COMMENT '操作人ID',
    username    VARCHAR(50) COMMENT '操作人账号',
    module      VARCHAR(50) COMMENT '模块',
    operation   VARCHAR(255) COMMENT '操作描述',
    method      VARCHAR(200) COMMENT '请求方法',
    cost_ms     INT COMMENT '耗时毫秒',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志';
