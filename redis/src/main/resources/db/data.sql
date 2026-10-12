-- ============================================================
-- 演示数据（全部 INSERT IGNORE，可重复执行）
-- 账号：admin/admin123（管理员）、zhangsan/123456、lisi/123456、wangwu/123456
-- 密码均为 BCrypt 密文，绝不明文存储
-- ============================================================
USE music_db;

-- ---------- 用户 ----------
INSERT IGNORE INTO sys_user (id, username, password, nickname, avatar, role, status, create_time) VALUES
 (1, 'admin',    '$2a$10$sSfA2oLx2qWRShvPXU3Bz.tuXLqHEFd6O//oI3AcvEIbPVyGIXZgu', '站点管理员', NULL, 'admin', 1, NOW()),
 (2, 'zhangsan', '$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '张三',     NULL, 'user',  1, NOW()),
 (3, 'lisi',     '$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '李四',     NULL, 'user',  1, NOW()),
 (4, 'wangwu',   '$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '王五',     NULL, 'user',  1, NOW()),
 (5, 'zhaoliu',  '$2a$10$P4UzFdJ2B7T0odwMxXOUpu04JuD.g2aKo8byStZ3X1YrduyLcto2K', '赵六(已禁用)', NULL, 'user', 0, NOW());

-- ---------- 歌曲 ----------
INSERT IGNORE INTO song (id, title, singer, album, category, duration, cover_url, audio_url, lyric, status, play_count, like_count, create_time) VALUES
 (1,  '夜空中最亮的星', '逃跑计划', '世界',       '摇滚', 252, 'https://picsum.photos/seed/song1/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3',  '夜空中最亮的星|能否听清|那仰望的人|心底的孤独和叹息', 1, 12800, 320, NOW()),
 (2,  '晴天',          '周杰伦',   '叶惠美',     '流行', 269, 'https://picsum.photos/seed/song2/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3',  '故事的小黄花|从出生那年就飘着|童年的荡秋千|随记忆一直晃到现在', 1, 9600, 288, NOW()),
 (3,  '海阔天空',      'Beyond',   '乐与怒',     '摇滚', 326, 'https://picsum.photos/seed/song3/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3',  '原谅我这一生不羁放纵爱自由|也会怕有一天会跌倒', 1, 8800, 265, NOW()),
 (4,  '小幸运',        '田馥甄',   '我的少女时代','流行', 233, 'https://picsum.photos/seed/song4/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3',  '原来你是我最想留住的幸运|原来我们和爱情曾经靠得那么近', 1, 7200, 210, NOW()),
 (5,  '成都',          '赵雷',     '无法长大',   '民谣', 328, 'https://picsum.photos/seed/song5/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3',  '和我在成都的街头走一走|直到所有的灯都熄灭了也不停留', 1, 6500, 198, NOW()),
 (6,  '起风了',        '买辣椒也用券','起风了',   '流行', 325, 'https://picsum.photos/seed/song6/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3',  '这一路上走走停停|顺着少年漂流的痕迹', 1, 6100, 176, NOW()),
 (7,  '演员',          '薛之谦',   '绅士',       '流行', 261, 'https://picsum.photos/seed/song7/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3',  '简单点说话的方式简单点|递进的情绪请省略', 1, 5400, 150, NOW()),
 (8,  'Shape of You',  'Ed Sheeran','÷',         '电子', 233, 'https://picsum.photos/seed/song8/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3',  'The club is not the best place to find a lover', 1, 5000, 143, NOW()),
 (9,  '月光奏鸣曲',    '贝多芬',   'Classic',    '古典', 480, 'https://picsum.photos/seed/song9/300/300',  'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3',  '（纯音乐，请欣赏）', 1, 3300, 98, NOW()),
 (10, '蓝色多瑙河',    '小约翰·施特劳斯','圆舞曲','古典', 560, 'https://picsum.photos/seed/song10/300/300', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3', '（纯音乐，请欣赏）', 1, 2600, 76, NOW()),
 (11, '孤勇者',        '陈奕迅',   '孤勇者',     '流行', 259, 'https://picsum.photos/seed/song11/300/300', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3', '爱你孤身走暗巷|爱你不跪的模样', 1, 1500, 52, NOW()),
 (12, '测试下架歌曲',  '测试歌手', '测试专辑',   '流行', 180, 'https://picsum.photos/seed/song12/300/300', 'https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3', '（管理员下架演示用）', 0, 120, 3, NOW());

-- ---------- 歌单 ----------
INSERT IGNORE INTO playlist (id, name, description, cover_url, user_id, status, play_count, create_time) VALUES
 (1, '每日30首：华语流行精选', '每天更新，陪你上下班',   'https://picsum.photos/seed/pl1/300/300', 1, 1, 3200, NOW()),
 (2, '深夜民谣：一个人的城市', '适合深夜循环',          'https://picsum.photos/seed/pl2/300/300', 2, 1, 1800, NOW()),
 (3, '摇滚不死：经典现场',     'Beyond / 逃跑计划',     'https://picsum.photos/seed/pl3/300/300', 1, 1, 1500, NOW()),
 (4, '古典入门：从月光开始',   '古典乐入门歌单',        'https://picsum.photos/seed/pl4/300/300', 2, 0, 600,  NOW()),
 (5, '待审核歌单（演示）',      '用户创建后需要管理员审核', 'https://picsum.photos/seed/pl5/300/300', 3, 2, 0,   NOW());

-- ---------- 歌单歌曲 ----------
INSERT IGNORE INTO playlist_song (id, playlist_id, song_id, sort_no) VALUES
 (1, 1, 2, 1), (2, 1, 4, 2), (3, 1, 6, 3), (4, 1, 7, 4), (5, 1, 11, 5),
 (6, 2, 5, 1), (7, 2, 6, 2),
 (8, 3, 1, 1), (9, 3, 3, 2),
 (10, 4, 9, 1), (11, 4, 10, 2);

-- ---------- 评论 ----------
INSERT IGNORE INTO comment (id, song_id, user_id, content, like_count, status, create_time) VALUES
 (1, 1, 2, '每次低谷的时候都会听这首', 12, 1, NOW()),
 (2, 1, 3, '前奏一起就起鸡皮疙瘩',     8,  1, NOW()),
 (3, 2, 2, '青春啊，回不去的夏天',     20, 1, NOW()),
 (4, 5, 3, '玉林路的尽头，小酒馆的门口', 15, 1, NOW()),
 (5, 3, 4, 'Beyond 永远的神',          9,  1, NOW());

-- ---------- 收藏 ----------
INSERT IGNORE INTO favorite (id, user_id, song_id) VALUES
 (1, 2, 1), (2, 2, 2), (3, 2, 5),
 (4, 3, 3), (5, 3, 5);
