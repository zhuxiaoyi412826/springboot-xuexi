-- =========================================================
-- 自行车售卖网站 数据库表结构（MySQL）
-- 由 Spring Boot 启动时自动执行（spring.sql.init.mode=always）
-- =========================================================

-- 用户表（普通用户 + 后台管理员共用，用 role 区分）
CREATE TABLE IF NOT EXISTS t_user (
    id          BIGINT       PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
    password    VARCHAR(100) NOT NULL COMMENT '登录密码',
    nickname    VARCHAR(50)  COMMENT '昵称/姓名',
    phone       VARCHAR(20)  COMMENT '手机号',
    address     VARCHAR(200) COMMENT '收货地址',
    role        VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT '角色：USER 普通用户 / ADMIN 管理员',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户表';

-- 自行车商品表
CREATE TABLE IF NOT EXISTS t_bike (
    id          BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100)   NOT NULL COMMENT '商品名称',
    brand       VARCHAR(50)    COMMENT '品牌',
    category    VARCHAR(30)    COMMENT '分类：山地车/公路车/折叠车/城市车/儿童车',
    price       DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '单价',
    stock       INT            NOT NULL DEFAULT 0 COMMENT '库存',
    status      TINYINT        NOT NULL DEFAULT 1 COMMENT '状态：1 在售 0 下架',
    image_url   VARCHAR(300)   COMMENT '图片地址（可为空，前端用图标兜底）',
    description VARCHAR(500)   COMMENT '商品描述',
    create_time DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    UNIQUE KEY uk_bike_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '自行车商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS t_order (
    id           BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(40)    NOT NULL COMMENT '订单编号',
    user_id      BIGINT         NOT NULL COMMENT '下单用户 id',
    username     VARCHAR(50)    COMMENT '下单用户名（冗余展示）',
    bike_id      BIGINT         NOT NULL COMMENT '自行车 id',
    bike_name    VARCHAR(100)   COMMENT '自行车名称（冗余展示）',
    price        DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '下单时单价快照',
    quantity     INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    receiver     VARCHAR(50)    COMMENT '收货人',
    phone        VARCHAR(20)    COMMENT '联系电话',
    address      VARCHAR(200)   COMMENT '收货地址',
    status       VARCHAR(20)    NOT NULL DEFAULT '待发货' COMMENT '订单状态：待发货/已发货/已完成/已取消',
    create_time  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单表';

-- =========================================================
-- 下面是「MP 实验室」需要的表 / 列
-- 加列用的是「先判断 information_schema 再 ALTER」的写法，重复启动也不会报错
-- =========================================================

-- 1) t_bike 加乐观锁版本号
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_bike' AND COLUMN_NAME = 'version');
SET @ddl := IF(@exist = 0, 'ALTER TABLE t_bike ADD COLUMN version INT NOT NULL DEFAULT 0 COMMENT ''乐观锁版本号''', 'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) t_user / t_order 加 update_time（自动填充要用）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_user' AND COLUMN_NAME = 'update_time');
SET @ddl := IF(@exist = 0, 'ALTER TABLE t_user ADD COLUMN update_time DATETIME NULL DEFAULT NULL COMMENT ''更新时间''', 'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 't_order' AND COLUMN_NAME = 'update_time');
SET @ddl := IF(@exist = 0, 'ALTER TABLE t_order ADD COLUMN update_time DATETIME NULL DEFAULT NULL COMMENT ''更新时间''', 'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) 多租户演示表（TenantLineInnerInterceptor 只对它生效）
CREATE TABLE IF NOT EXISTS t_tenant_goods (
    id         BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    tenant_id  BIGINT        NOT NULL DEFAULT 1 COMMENT '租户 id（由插件自动写入/拼接）',
    name       VARCHAR(100)  NOT NULL COMMENT '商品名',
    price      DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
    stock      INT           NOT NULL DEFAULT 0 COMMENT '库存',
    create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '多租户演示表';

-- 4) 动态表名演示：订单归档表（结构和 t_order 一致）
CREATE TABLE IF NOT EXISTS t_order_2026 (
    id           BIGINT         PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    order_no     VARCHAR(40)    NOT NULL COMMENT '订单编号',
    user_id      BIGINT         NOT NULL COMMENT '下单用户 id',
    username     VARCHAR(50)    COMMENT '下单用户名（冗余展示）',
    bike_id      BIGINT         NOT NULL COMMENT '自行车 id',
    bike_name    VARCHAR(100)   COMMENT '自行车名称（冗余展示）',
    price        DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '下单时单价快照',
    quantity     INT            NOT NULL DEFAULT 1 COMMENT '购买数量',
    total_amount DECIMAL(10, 2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
    receiver     VARCHAR(50)    COMMENT '收货人',
    phone        VARCHAR(20)    COMMENT '联系电话',
    address      VARCHAR(200)   COMMENT '收货地址',
    status       VARCHAR(20)    NOT NULL DEFAULT '待发货' COMMENT '订单状态',
    create_time  DATETIME       DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME       NULL DEFAULT NULL COMMENT '更新时间',
    deleted      TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 1 已删除',
    UNIQUE KEY uk_order_no_2026 (order_no)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '订单归档表（动态表名演示）';

-- 5) 批量插入压测表
CREATE TABLE IF NOT EXISTS t_batch_demo (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    name        VARCHAR(100)  NOT NULL COMMENT '名称',
    price       DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
    create_time DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '批量插入压测表';
