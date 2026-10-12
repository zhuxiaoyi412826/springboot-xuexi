# 在线听歌网站（SpringBoot 4 + MyBatis-Plus + Sa-Token + Redis）

一个**能真正用起来**的在线听歌网站，同时是一份** Redis 学习工程**：
业务上实现「听歌 + 用户/管理双端」；技术上把 Redis 的每种数据结构、每种缓存问题
（穿透 / 击穿 / 雪崩）、以及 L1/L2/L3 多级缓存，都做成了**可以点、可以看到数据变化**的接口和页面。

- 端口：`8085`
- 接口文档（Knife4j）：<http://localhost:8085/doc.html>
- 登录页（用户端/管理端共用）：<http://localhost:8085/login.html>

---

## 1. 快速开始

### 1.1 环境要求

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21+ | Boot 4 要求 |
| Maven | 3.9+ | 已配置阿里云镜像 |
| MySQL | 8.x | 账号 `root` / 密码 `412826` |
| Redis | 5.0+（本机 5.0.14） | 无密码 |

### 1.2 启动

```bash
cd d:\daima\Lianshi\springboot-xuexi\redis
mvn -B package -DskipTests
java -jar target/redis-0.0.1-SNAPSHOT.jar
```

首次启动会自动执行 `src/main/resources/db/schema.sql`（建库建表）和 `data.sql`（12 首歌、4 个用户、歌单、评论、收藏），
不需要手动导数据。启动后控制台会打印「布隆过滤器就绪 / 预热完成」。

> 不想自动初始化：把 `application.yaml` 里的 `spring.sql.init.mode` 改成 `never`，手动执行 `db/` 下两个 SQL。

### 1.3 演示账号

| 用户名 | 密码 | 角色 | 登录后跳转 |
|---|---|---|---|
| `admin` | `admin123` | admin | `/admin/index.html`（管理端） |
| `zhangsan` | `123456` | user | `/user/index.html`（用户端） |
| `lisi` | `123456` | user | `/user/index.html` |
| `wangwu` | `vip888` | user | `/user/index.html` |

登录成功后后端返回 `{token, tokenName, user, home}`，前端把 token 存 localStorage，
后续请求放在请求头 `satoken` 里（Sa-Token 的 `token-name` 配置）。

---

## 2. 功能清单

### 2.1 用户端（`static/user/`）

| 页面 | 功能 |
|---|---|
| `index.html` | 发现音乐：分类筛选、歌曲卡片、分页、**底部播放器**、热歌榜 |
| `song.html` | 歌曲详情、歌词、播放、点赞、收藏、评论（限流 + 幂等） |
| `rank.html` | 热歌榜（ZSet）、热搜榜（ZSet） |
| `search.html` | 搜索、搜索历史（List）、热搜跳转 |
| `me.html` | 我的资料、签到（Bitmap）、收藏（Set）、最近播放（List） |
| `playlist.html` | 歌单详情（歌曲列表存 Redis List）、公开歌单列表 |

### 2.2 管理端（`static/admin/`）

| Tab | 功能 |
|---|---|
| 概览 | 缓存命中率、L1 指标、Redis INFO（keys/内存/命中率） |
| 歌曲管理 | 新增/上下架/删除（同步删缓存、同步布隆、下架摘榜单），可投递「30 秒后自动上架」延迟任务 |
| 用户管理 | 禁用/启用（立刻踢下线）、强制下线、封禁 N 分钟、在线用户（Redis Set） |
| 评论管理 | 屏蔽/放开/删除 |
| 歌单审核 | 待审核队列（Redis List）→ 通过/拒绝 |
| 缓存 / Redis | 按缓存名清理、手动预热、L1/L2/布隆动态开关、SCAN 浏览 key、Pub/Sub 通知 |
| Redis 实验室 | 每个按钮对应一条真实 Redis 命令（见第 4 节） |

---

## 3. L1 / L2 / L3 多级缓存设计

```text
请求 → L1 Caffeine（进程内，60s）
     → L2 Redis（分布式，600s ± 随机抖动）
     → L3 MySQL（回源，写回 L2、L1）
```

实现在 `cache/MultiLevelCacheService.java`，开关在 `application.yaml` 的 `music.cache.*`，
也可以在管理端「缓存 / Redis」Tab 里**在线开关**（改的是内存里的配置，重启回到配置文件的值）。

### 3.1 三个经典问题的解法（都做了，都能验证）

| 问题 | 现象 | 本项目的做法 | 在哪里验证 |
|---|---|---|---|
| **缓存穿透** | 查一个根本不存在的 id，请求直达 DB | ① 布隆过滤器拦截（Redisson `RBloomFilter` 思路自实现：BitMap + 多 hash）② 查不到也缓存空值 60s | 访问不存在的歌曲 id；管理端关掉布隆再对比 |
| **缓存击穿** | 热点 key 过期瞬间，大量请求同时回源 | 热点 key 走**逻辑过期**：缓存永不过期，过期后只放一个线程回源（Redis 互斥锁 `SET NX PX`），其余线程先返回旧值 | `GET /api/songs/{id}`；`music.cache.logical-expire-seconds` |
| **缓存雪崩** | 大批 key 同一时刻过期 | 过期时间加**随机抖动**（`l2-jitter-seconds`）+ 启动时**预热** | `WarmUpService`（ApplicationRunner） |

### 3.2 L1 与 L2 的一致性

L1 是进程内缓存，多实例之间不会自动同步。本项目用 **Redis Pub/Sub 广播失效消息**
（`music:channel:cache:invalidate`），删除 L2 时顺便广播，其他实例收到后删掉自己的 L1
（见 `MultiLevelCacheService.evict`）。单实例演示时看不出效果，多开一个端口实例就能看到。

### 3.3 命中率去哪看

- 管理端「概览」Tab：`cacheStats`（按缓存维度 + `_summary` 汇总）、`l1`（Caffeine 命中率）、`redis`（INFO 的 keyspace_hits/misses）
- `CacheStatsService` 用 `LongAdder` 统计，避免并发计数成为瓶颈

### 3.4 数据一致性策略

改库（新增/修改/删除/上下架）一律**先删缓存，下次读取自动回源**（Cache-Aside），
`SongService.add/update/delete/setStatus` 里都能看到；列表缓存用 `cache.clear("songList")` 整类清理。

---

## 4. Redis 知识点地图（每种数据结构都有真实业务 + 可点接口）

| 类型 | 业务场景 | 关键命令 | 实验室接口（免登录） |
|---|---|---|---|
| **String** | token、计数器（播放量/点赞数）、分布式锁、幂等 | `SET/SETEX/SETNX/INCR` | `/api/lab/string/set`、`/incr`、`/setnx` |
| **Hash** | 歌单元信息、对象属性局部更新 | `HSET/HGETALL/HINCRBY` | `/api/lab/hash/set`、`/hash/incr` |
| **List** | 最近播放、搜索历史、歌单歌曲列表、待审核队列、消息队列 | `LPUSH/LTRIM/LRANGE/RPOP` | `/api/lab/list/push`、`/list/pop` |
| **Set** | 点赞用户、收藏、共同收藏（交集）、在线用户 | `SADD/SREM/SISMEMBER/SCARD/SINTER` | `/api/lab/set/add`、`/set/inter` |
| **ZSet** | 热歌榜、热搜榜、延迟队列（score=执行时间） | `ZADD/ZINCRBY/ZREVRANGE/ZREMRANGEBYSCORE` | `/api/lab/zset/add`、`/zset/incr`、`/zset/top` |
| **Bitmap** | 签到、日活（一个 bit 一个人/一天） | `SETBIT/GETBIT/BITCOUNT` | `/api/lab/bitmap/set`、`/bitmap/get` |
| **HyperLogLog** | UV 去重计数（12KB 固定内存，误差 0.81%） | `PFADD/PFCOUNT` | `/api/lab/hll/add` |
| **Geo** | 附近的歌友 | `GEOADD/GEORADIUS/GEOPOS` | `/api/lab/geo/add`、`/geo/near` |
| **Stream** | 播放流水（可回溯、不丢消息，消费者异步落库） | `XADD/XREAD/XLEN` | `/api/lab/stream/add`、`/stream/read` |
| **Pub/Sub** | 缓存失效广播、站内通知 | `PUBLISH/SUBSCRIBE` | `/api/lab/pubsub/pub` |
| **Lua** | 令牌桶限流、释放锁（保证原子性） | `EVAL` | `/api/lab/ratelimit`（连点直到返回 0）、`/api/lab/lock` |
| **Pipeline** | 批量写/预热（减少网络往返） | `MULTI-EXEC` 无原子性版 | `/api/lab/pipeline?n=200`（逐条 vs pipeline 耗时对比） |
| **事务** | 一起执行 | `MULTI/EXEC` | `/api/lab/tx` |
| **延迟队列** | 30 秒后自动上架歌曲 | `ZADD + ZRANGEBYSCORE + ZREM`（Lua 保证原子取） | `/api/lab/delay/add`、`/delay/list` |
| **分布式 ID** | 评论/订单主键（INCR 原子） | `INCR/INCRBY`（号段模式） | `/api/lab/id/next`、`/id/range` |
| **布隆过滤器** | 防缓存穿透 | 自己实现：BitMap + 多次 hash | `/api/lab/bloom/add`、`/bloom/exists` |
| **运维** | 查 key、看 TTL、删 key | `SCAN/TYPE/TTL/DEL`（**禁止 KEYS \***） | `/api/lab/keys`、`/ttl`、`/del` |

### 4.1 两个真实链路示例

**播放一首歌（`SongService.play`）** —— 一次点击联动 6 个 Redis 结构：

```text
INCR  music:cnt:play:{songId}      # 播放量计数（不直接 UPDATE 数据库）
ZINCRBY music:rank:hot songId 1    # 热歌榜加分
XADD  music:stream:play * ...      # 流水写 Stream，消费者异步落库（削峰）
LPUSH music:recent:play:{userId}   # 最近播放，只保留 20 条
SETBIT music:active:{yyyyMMdd} uid # 日活 Bitmap
PFADD music:uv:{yyyyMMdd} uid      # UV 去重
```

**发一条评论（`CommentService.add`）** —— 三级防护：

```text
Lua 令牌桶限流 → 幂等 SETNX（10 秒内同一 key 只处理一次）→ INCR 生成分布式 ID
```

---

## 5. 目录结构

```text
redis/
├── src/main/java/org/example/redis/
│   ├── RedisApplication.java          # 启动类（@MapperScan + @EnableScheduling）
│   ├── config/                        # MybatisPlus、Redis、Sa-Token、SaTokenDao、Knife4j、ObjectMapper
│   ├── common/                        # Result、PageResult、BizException、全局异常处理
│   ├── entity/ + mapper/ + vo/        # 实体 / MyBatis-Plus Mapper / 出参视图
│   ├── redis/RedisKeys.java           # 所有 key 的命名规范（统一前缀 music:）
│   ├── redis/RedisService.java        # Redis 命令统一封装（200 行工具类，注释即教程）
│   ├── cache/                         # 多级缓存 + 缓存工具
│   │   ├── MultiLevelCacheService     # L1/L2/L3 + 逻辑过期 + 互斥锁 + 空值 + 布隆
│   │   ├── CacheProperties/CacheLevel/CacheStatsService
│   │   ├── RedisLockService           # SET NX PX + Lua 释放锁
│   │   ├── BloomFilterService         # 自实现布隆过滤器
│   │   ├── RateLimiterService         # Lua 令牌桶
│   │   ├── IdGeneratorService         # INCR / 号段
│   │   └── DelayQueueService          # ZSet 延迟队列
│   ├── service/                       # Song/Playlist/Comment/Search/Sign/Stat/User
│   │   ├── WarmUpService              # 启动预热（ApplicationRunner）
│   │   ├── PlayEventConsumer          # Stream 消费者 + 播放量回写（@Scheduled）
│   │   ├── CacheManageService         # 管理端运维面板
│   │   └── DelayTaskRunner            # 延迟队列轮询（@Scheduled）
│   ├── controller/                    # 用户端接口 + admin/ 管理端接口
│   └── listener/NoticeListener.java   # Pub/Sub 订阅
├── src/main/resources/
│   ├── application.yaml               # 全部配置（含 music.cache.* 中文注释）
│   ├── db/schema.sql + data.sql       # 自动初始化
│   └── static/                        # index/login + css/js + user/ + admin/
└── tools/smoke-music.ps1              # 一键冒烟测试（60+ 项）
```

---

## 6. 接口清单

### 6.1 公共 / 登录

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/public/ping` | 连通性检查（含 Redis PONG） |
| POST | `/api/auth/login` | 登录，返回 token + 角色 + 落地页 |
| POST | `/api/auth/register` | 注册（默认 user） |
| POST | `/api/auth/logout` | 退出 |
| GET | `/api/auth/info` | 当前用户、角色、权限、token 剩余时间 |

### 6.2 歌曲 / 评论 / 歌单 / 搜索 / 个人

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/songs/list` | 列表（三级缓存，支持 keyword/category） |
| GET | `/api/songs/hot` | 热歌榜（ZSet） |
| GET | `/api/songs/{id}` | 详情（热点 key：逻辑过期） |
| POST | `/api/songs/{id}/play` | 播放（计数 + 榜单 + 最近播放 + Stream + 日活/UV） |
| POST | `/api/songs/{id}/like` | 点赞/取消（Set） |
| POST | `/api/songs/{id}/favorite` | 收藏/取消（Set + DB） |
| GET | `/api/songs/favorites` | 我的收藏 |
| GET | `/api/songs/recent` | 最近播放（List） |
| GET | `/api/songs/common-favorite/{userId}` | 共同收藏（Set 交集） |
| GET/POST | `/api/comments/...` | 评论列表/发表/点赞/删除 |
| GET/POST | `/api/playlists/...` | 歌单列表/详情/创建（进审核队列）/加歌 |
| GET | `/api/search` `/hot` `/history` | 搜索 / 热搜榜 / 搜索历史 |
| GET/POST | `/api/me/profile` `/sign` `/location` `/nearby` `/stats` | 资料 / 签到 / Geo / UV 日活 |

### 6.3 管理端（需 admin 角色，注解 `@SaCheckRole("admin")`）

| 方法 | 路径 |
|---|---|
| GET/POST/DELETE | `/api/admin/songs/list`、`/add`、`/update`、`/{id}`、`/{id}/status` |
| GET/POST | `/api/admin/users/list`、`/{id}/status`、`/{id}/kickout`、`/{id}/ban`、`/{id}/unban`、`/online` |
| GET/POST | `/api/admin/comments/list`、`/{id}/shield`、`/{id}/delete` |
| GET/POST | `/api/admin/playlists/list`、`/audit-queue`、`/{id}/audit` |
| GET/POST/DELETE | `/api/admin/cache/overview`、`/clear`、`/warm`、`/level`、`/keys`、`/key`、`/reset-stats`、`/notices` |

### 6.4 权限规则（`config/SaTokenConfig.java`）

```text
/api/public/**        放行
/api/auth/**          放行
/admin/**             需要登录 + admin 角色（页面级拦截）
/api/admin/**         需要登录 + admin 角色
/api/**               其余接口需要登录
```

未登录 → 401 JSON；登录了但角色不够 → 403 JSON（`GlobalExceptionHandler` 统一处理）。

---

## 7. 数据库表

| 表 | 作用 |
|---|---|
| `sys_user` | 用户（BCrypt 密码、角色、状态、签到天数） |
| `song` | 歌曲（标题、歌手、音频地址、封面、播放量、点赞数、状态） |
| `playlist` / `playlist_song` | 歌单与歌单歌曲关系 |
| `comment` | 评论（状态：1 正常 / 0 屏蔽） |
| `favorite` | 收藏（Redis Set 为主，DB 落库） |
| `play_record` | 播放流水（Stream 消费者异步写入） |

---

## 8. 学习建议：怎么玩这个项目

1. **开一个 `redis-cli MONITOR`**，然后点页面上的播放/点赞/搜索，看真实的 Redis 命令流。
2. **看命中率**：管理端「概览」页，多刷几次歌曲列表，观察 L1 命中率上升；关掉 L2 再刷，观察回源。
3. **制造缓存击穿**：`DEL music:cache:song:1`（或用管理端删 key），立刻并发请求详情，看日志里只有一个线程回源。
4. **制造穿透**：请求不存在的 `/api/songs/99999`，看布隆过滤器日志；再关掉布隆对比 DB 查询次数。
5. **看延迟任务**：管理端歌曲列表点「30 秒后自动上架」，然后 `ZRANGE music:delay:queue 0 -1 WITHSCORES`，
   30 秒后观察后台日志「延迟任务执行成功」。
6. **看限流**：`curl` 连续请求 10 次 `/api/lab/ratelimit?biz=demo&capacity=5&refill=1`，第 6 次开始返回 0。
7. **看 Pipeline 差距**：`/api/lab/pipeline?n=200`，普通逐条写 vs pipeline 的毫秒数对比非常直观。

---

## 9. 踩坑记录（Boot 4 / Redis 5 环境）

| 现象 | 原因 | 解决 |
|---|---|---|
| `No qualifying bean of type ObjectMapper` | Boot 4 的 `spring-boot-starter-webmvc` 不再自带 Jackson 自动配置 | 新增 `spring-boot-starter-jackson` + 在 `RedisConfig` 里手写 `ObjectMapper` Bean（注册 `JavaTimeModule`，否则 `LocalDateTime` 序列化报错） |
| 登录 500 `Error in execution` | Sa-Token 的 Redis DAO 用 `SET key val KEEPTTL`，**Redis 6.0 才有**，本机是 5.0.14 | `SaTokenDaoConfig` 里继承 `SaTokenDaoForRedisTemplate`，重写 `setStringAndKeepTTL/update` 为「读 TTL → SET → 补 EXPIRE」 |
| `SaBeanInject.setSaTokenDao required a single bean, but 2 were found` | starter 自己也注册了一个 DAO Bean | 自定义 DAO Bean 加 `@Primary`（见 `SaTokenDaoConfig`） |
| `RedisCallback` 找不到符号 | 它在 `org.springframework.data.redis.core`，不在 `.connection` | 改 import |
| `opsForSet().add(key, (Object[]) members)` 编译不过 | `StringRedisTemplate` 的泛型是 `String` | 直接传 `String...` |
| `DataType` 无法转 `String` | `type()` 返回枚举 | `dataType.code()` |
| 后台管理报错「无法反序列化的类型：org.example.redis.vo.UserVO，请先将其注册到 JSON 全局类型白名单」 | Sa-Token 存 Session 用 Jackson 的 `@class` 默认类型，**只允许白名单类型** | 在 `RedisApplication` 静态块里 `SaJsonStrategy.instance.registerAllowType(UserVO.class)`（必须在 Sa-Token 初始化前） |
| PowerShell 脚本里的中文和 `&` 报语法错 | PS 5.1 把无 BOM 的 UTF-8 当 GBK 解析 | 给 `.ps1` 加 UTF-8 BOM |

---

## 10. 冒烟测试

```powershell
powershell -ExecutionPolicy Bypass -File tools\smoke-music.ps1
```

覆盖：连通性、双角色登录、歌曲/播放/点赞/收藏、评论限流与幂等、搜索/签到/统计、
权限（401/403）、缓存面板、Redis 实验室 30+ 个命令、管理端业务。
结果形如 `PASS=xx FAIL=0`。
