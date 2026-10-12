-- ============================================================
-- 初始化数据（幂等：INSERT IGNORE，可重复执行）
-- 说明：密码全部是密文，绝不明文
--   admin    / admin123  （{bcrypt}，管理员）
--   zhangsan / 123456    （{bcrypt}，普通用户）
--   lisi     / 123456    （{bcrypt}，普通用户 + VIP）
--   wangwu   / vip888    （{bcrypt}，普通用户）
--   bob      / 123456    （{argon2}，演示另一种加密算法）
--   carol    / 123456    （{pbkdf2}，演示第三种加密算法）
--   locked   / 123456    （已禁用账号，用于演示「账号禁用」）
-- ============================================================

USE video_site;

-- ---------- 用户 ----------
INSERT IGNORE INTO sys_user (id, username, password, digest_hash, nickname, email, source, enabled, account_non_locked, login_fail_count, create_time)
VALUES
 (1, 'admin',    '{bcrypt}$2a$10$sSfA2oLx2qWRShvPXU3Bz.tuXLqHEFd6O//oI3AcvEIbPVyGIXZgu', 'fd4bf320b5e82041dc627c83964166cc', '站点管理员', 'admin@video.site',   'local', 1, 1, 0, NOW()),
 (2, 'zhangsan', '{bcrypt}$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', 'fbdf2324f5aa1e5cd30c4c28b1b1b8af', '张三',       'zhangsan@video.site','local', 1, 1, 0, NOW()),
 (3, 'lisi',     '{bcrypt}$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '495b9b4d1b195191c6a734ab797ed6b8', '李四(VIP)', 'lisi@video.site',    'local', 1, 1, 0, NOW()),
 (4, 'wangwu',   '{bcrypt}$2a$10$OdbGqimWp5D9fIRSAY29s.M62ffG12/65oxaC3KF9jsAGX/bsJ2/y', 'f255cdc166e6182ee65f054bda3aa7b0', '王五',       'wangwu@video.site',  'local', 1, 1, 0, NOW()),
 (5, 'bob',      '{argon2}$argon2id$v=19$m=16384,t=2,p=1$0Bfo/IRERKb+j9O0V3j2kg$gPseI4/VwweIj7LxBlGutID0+O4Viy5mf/QQBay4AOs', 'af0af2671733614563d4191ca7125869', '鲍勃', 'bob@video.site', 'local', 1, 1, 0, NOW()),
 (6, 'carol',    '{pbkdf2}c4033f685d525b0b2a5cf455a104c3591e5fba3f9411a3aacd355c7a92ac68ffd0fad22a7b7d673a2b72ad88e47d223d', NULL, '卡罗尔', 'carol@video.site', 'local', 1, 1, 0, NOW()),
 (7, 'locked',   '{bcrypt}$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '9f4d17117e8ab50bc8cac2861a2e0585', '已禁用账号', 'locked@video.site', 'local', 0, 1, 0, NOW());

-- ---------- 角色 ----------
INSERT IGNORE INTO sys_user_role (user_id, role_code) VALUES
 (1, 'ROLE_ADMIN'), (1, 'ROLE_USER'),
 (2, 'ROLE_USER'),
 (3, 'ROLE_USER'), (3, 'ROLE_VIP'),
 (4, 'ROLE_USER'),
 (5, 'ROLE_USER'),
 (6, 'ROLE_USER'),
 (7, 'ROLE_USER');

-- ---------- 视频 ----------
INSERT IGNORE INTO video (id, title, description, cover_url, video_url, category, tags, duration_sec, author_id, author_name, status, view_count, like_count, create_time)
VALUES
 (1, '大雄兔 Big Buck Bunny', '经典开源动画短片，画质清晰，适合测试播放器。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4', '影视', '动画,开源,经典', 596, 1, '站点管理员', 'PUBLIC', 1200, 88, NOW()),
 (2, '大象之梦 Elephants Dream', '开源 3D 动画电影片段。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4', '影视', '3D,开源', 653, 1, '站点管理员', 'PUBLIC', 860, 45, NOW()),
 (3, '钢铁之泪 Tears of Steel', 'Blender 基金会开源科幻短片。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4', '影视', '科幻,开源', 734, 1, '站点管理员', 'PUBLIC', 730, 39, NOW()),
 (4, '辛特尔 Sintel', '开源动画短片《辛特尔》。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4', '影视', '动画,奇幻', 888, 1, '站点管理员', 'PUBLIC', 640, 30, NOW()),
 (5, 'GTI 评测：城市与山地', '汽车评测短片，演示高码率视频。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/VolkswagenGTIReview.mp4', '汽车', '评测,汽车', 150, 2, '张三', 'PUBLIC', 420, 21, NOW()),
 (6, '斯巴鲁越野实录', '越野路况实录。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/SubaruOutbackOnStreetAndDirt.mp4', '汽车', '越野,实录', 594, 2, '张三', 'PUBLIC', 380, 18, NOW()),
 (7, '一分钟看懂 HTTP 缓存', '前端性能优化系列：强缓存、协商缓存实战。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4', '科技', 'HTTP,前端,缓存', 60, 2, '张三', 'PUBLIC', 990, 76, NOW()),
 (8, 'Spring Security 过滤器链详解', '从 FilterChainProxy 到各个 Filter 的执行顺序。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4', '教育', 'Spring,Security,Java', 60, 2, '张三', 'PUBLIC', 1500, 132, NOW()),
 (9, '我的私藏：未公开视频', '只有作者本人或管理员可见（对象级权限演示）。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4', '生活', '私有,权限演示', 60, 2, '张三', 'PRIVATE', 12, 1, NOW()),
 (10,'待审核：新上传视频', '管理员审核通过后才会公开（数据权限演示）。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4', '生活', '审核中', 60, 2, '张三', 'REVIEW', 3, 0, NOW()),
 (11,'十分钟入门 Redis', 'Redis 数据类型与典型场景。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4', '教育', 'Redis,缓存', 60, 3, '李四(VIP)', 'PUBLIC', 700, 58, NOW()),
 (12,'吉他入门：三个和弦', '零基础吉他教学。', NULL,
     'https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4', '音乐', '吉他,教学', 60, 3, '李四(VIP)', 'PUBLIC', 520, 41, NOW());

-- ---------- 评论 ----------
INSERT IGNORE INTO comment (id, video_id, user_id, username, content, status, create_time) VALUES
 (1, 1, 2, 'zhangsan', '画质真好，播放器秒开！', 0, NOW()),
 (2, 1, 3, 'lisi',     'VIP 下载也很快。', 0, NOW()),
 (3, 8, 2, 'zhangsan', '过滤器链讲得很清楚，收藏了。', 0, NOW()),
 (4, 8, 4, 'wangwu',   '期待讲一下 OAuth2 资源服务器。', 0, NOW());

-- ---------- 收藏 ----------
INSERT IGNORE INTO favorite (id, user_id, video_id, create_time) VALUES
 (1, 2, 1, NOW()),
 (2, 2, 8, NOW()),
 (3, 3, 11, NOW());
