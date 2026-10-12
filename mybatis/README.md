# 小说阅读器 · MyBatis 学习项目

一个融合后台管理系统与前台阅读功能的完整 Web 项目，用 **Spring Boot 4 + 原生 MyBatis** 实现，
目标是**把 MyBatis 的主要知识点都落到真实业务场景里跑一遍**。

功能覆盖：上传、浏览（在线阅读）、下载（可暂停/继续/停止）、收藏、阅读历史（书架进度）、用户管理、统计报表。

---

## 一、技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | |
| Spring Boot | 4.1.1 | |
| MyBatis | 3.5.19 | `mybatis-spring-boot-starter 4.0.1`（适配 Spring Framework 7 的版本） |
| MySQL | 8.x（Connector/J 9.7） | |
| 前端 | 原生 HTML + CSS + JS | 无框架，REST API 交互 |
| 端口 | **8082** | 避免与本机其他项目冲突 |

---

## 二、快速开始

### 1. 准备数据库

只要本机 MySQL 已启动，**无需手工执行任何 SQL**。启动时自动完成：

1. 库不存在则创建 `novel_reader`（连接串带 `createDatabaseIfNotExist=true`）
2. 执行 `db/schema.sql` 建 8 张表（`CREATE TABLE IF NOT EXISTS`）
3. 执行 `db/data.sql` 灌入演示数据（`INSERT IGNORE`，重启不覆盖你的改动）

账号密码在 `application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/novel_reader?createDatabaseIfNotExist=true&useUnicode=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false
    username: root
    password: '412826'
```

### 2. 启动

```powershell
cd d:\daima\Lianshi\springboot-xuexi\mybatis
.\mvnw.cmd spring-boot:run
```

或打包运行：

```powershell
.\mvnw.cmd -DskipTests package
java -jar target/mybatis-0.0.1-SNAPSHOT.jar
```

### 3. 访问

<http://localhost:8082>

---

## 三、页面与演示账号

| 页面 | 路径 | 说明 |
|---|---|---|
| 登录 / 注册 | `/login.html` | 根路径 `/` 会 302 跳到这里 |
| 书城首页 | `/index.html` | 搜索/筛选/分页/详情/收藏/下载 |
| 在线阅读器 | `/reader.html?novelId=x&chapterId=y` | 目录、正文、上一章/下一章、自动记录进度 |
| 我的下载 | `/downloads.html` | 下载任务的暂停/继续/停止 + 书架 |
| 后台管理 | `/admin.html` | 7 大模块，仅管理员 |

| 账号 | 密码 | 角色 | 登录后 |
|---|---|---|---|
| `admin` | `123456` | ADMIN | `/admin.html` |
| `zhangsan` | `123456` | USER | `/index.html` |
| `lisi` | `123456` | USER | `/index.html` |

注册页面只能注册**普通用户**，防止有人注册管理员。

---

## 四、功能清单

### 前台

- **浏览**：关键词检索书名/作者/简介、分类筛选、连载状态筛选、6 种排序
- **阅读**：左右方向键翻页、自动记录阅读进度、显示最新章节
- **收藏**：收藏/取消收藏，实时联动小说的收藏计数
- **下载**：创建任务后可**暂停 / 继续 / 停止 / 重新下载**，页面 1.2 秒自动刷新进度
- **书架**：阅读过的书 + 进度百分比 + 继续阅读位置

### 后台（7 个模块）

| 模块 | 内容 |
|---|---|
| 概况统计 | 12 个核心指标 + 分类统计 + 作者排行 + 用户活跃排行 |
| 小说管理 | 分页、多条件检索、新增/编辑/删除、批量删除 |
| 章节管理 | 按小说查看目录、分页、新增/编辑/删除章节 |
| 上传小说 | TXT 上传自动分章（支持 UTF-8 / GBK 自动识别） |
| 分类管理 | CRUD，删除前校验是否还有小说 |
| 用户管理 | 分页检索、禁用/启用、重置密码、删除、批量删除 |
| 下载任务 | 全局任务监控，可按状态筛选 |

---

## 五、MyBatis 知识点全景

> 这是本项目最核心的部分：每个知识点都对应到具体的代码位置，可以直接打开对照看。

### 1. 注解 vs XML 的职责划分

| 写法 | 用在哪 | 文件 |
|---|---|---|
| **注解**（`@Select/@Insert/@Update/@Delete`） | 简单单表 CRUD | `CategoryMapper`（纯注解）<br>`SysLogMapper`（纯注解）<br>`FavoriteMapper` / `ReadingHistoryMapper` / `DownloadTaskMapper` 的简单操作 |
| **XML 映射文件** | 复杂 SQL（多表、动态、批量、聚合） | `resources/mapper/*.xml`（8 个文件） |

> 同一个方法**不能**既写注解又配 XML，否则启动报 `MappedStatement already exists`。

### 2. 动态 SQL（`mapper/NovelMapper.xml` 最集中）

| 标签 | 出现位置 | 用途 |
|---|---|---|
| `<sql>` + `<include>` | `NovelMapper.xml` | 抽取查询列、WHERE 条件、ORDER BY 片段复用 |
| `<if>` | 所有 XML | 条件成立才拼接该段 SQL |
| `<where>` | `NovelMapper`、`DownloadTaskMapper` | 自动去掉多余的 `AND/OR` |
| `<trim prefix="WHERE" prefixOverrides>` | `ChapterMapper.xml` | 比 `<where>` 更灵活的前缀控制 |
| `<choose>/<when>/<otherwise>` | `NovelMapper`、`UserMapper` | 动态排序白名单，**杜绝 SQL 注入** |
| `<foreach>` | `ChapterMapper`（批量插入）<br>`FavoriteMapper`（IN 查询）<br>`UserMapper`（批量删除） | 遍历集合 |
| `<set>` | `NovelMapper.updateSelective` | 动态更新，自动去掉末尾逗号 |
| `<bind>` | `ChapterMapper.selectByCondition` | 把拼接好的模糊串存成变量 |

### 3. 分页（自研物理分页插件）

没有引第三方 PageHelper，而是**自己实现了 和 PageHelper 同原理的分页插件**：

- `plugin/PageContext` — ThreadLocal 存放分页参数
- `plugin/PageInterceptor` — 拦截 `Executor#query`：
  1. 把原 SQL 包一层执行 `COUNT(*)` 算出 total
  2. 在原 SQL 后追加 `LIMIT offset,size`
- `config/MyBatisConfig` — 把插件注册成 bean（MyBatis Starter 会自动注入 SqlSessionFactory）

用法：

```java
PageContext.start(pageNum, pageSize);
List<NovelVO> list = mapper.selectByCondition(query);
long total = PageContext.getTotal();
PageContext.clear();     // 务必在 finally 里清理
```

### 4. 多表查询

| 类型 | 位置 | 说明 |
|---|---|---|
| `<association>` 一对一 | `NovelMapper.xml` → `NovelDetailMap` | 小说详情里嵌套 `Category` 对象 |
| `<collection>` 一对多 | `CategoryMapper.xml` → `CategoryWithNovelsMap` | 一次查出分类及其下所有小说，避免 N+1 |
| JOIN + 标量子查询 | `NovelMapper.selectByCondition` | 小说 JOIN 分类，子查询取最新章节/是否收藏 |
| EXISTS 子查询 | `NovelMapper.xml` 的 `hasChapter` 条件 | 只查有章节的小说 |

### 5. SQL 函数（`StatMapper.xml` 集中演示）

`COUNT` / `SUM` / `AVG` / `MAX` / `MIN` / `IFNULL` / `ROUND` / `CONCAT` / `CASE WHEN` / `DATE_FORMAT` / `DATE_SUB` / `RAND` / `GROUP_CONCAT` 场景：
`GROUP BY` + `HAVING`（作者排行：只统计作品数 ≥ N 的作者）。

### 6. 缓存

| 类型 | 位置 | 说明 |
|---|---|---|
| 一级缓存 | SqlSession 级，默认开启 | 同一次会话内相同查询不重复打库 |
| 二级缓存 | `CategoryMapper` 的 `@CacheNamespace`<br>`NovelMapper.xml` 的 `<cache>` | namespace 级，跨会话共享 |
| 缓存控制 | `useCache="false"` / `flushCache` | 详情页、检索列表关闭缓存保证实时性 |

> 二级缓存的两个硬性要求：
> 1. 实体类必须实现 `Serializable`
> 2. 频繁变动的数据（浏览数、收藏数）**不要**缓存

### 7. 事务（`@Transactional(rollbackFor = Exception.class)`）

| 场景 | 位置 |
|---|---|
| 上传 TXT：小说 + 批量章节 + 回写统计，任一步失败全回滚 | `NovelServiceImpl.upload` |
| 收藏：收藏表 + 小说收藏计数必须同时成功 | `FavoriteServiceImpl.toggle` |
| 删除小说：级联删除章节、收藏、历史 | `NovelServiceImpl.remove` |
| 下载任务状态流转 + CAS 并发控制 | `DownloadServiceImpl` |

### 8. 插件（MyBatis Interceptor）

| 插件 | 文件 | 作用 |
|---|---|---|
| `PageInterceptor` | `plugin/PageInterceptor.java` | 物理分页（见第 3 点） |
| `SlowSqlInterceptor` | `plugin/SlowSqlInterceptor.java` | 慢 SQL 监控，超过阈值打印 WARN |

两者都拦截 `Executor#query`，演示了 MyBatis 四大对象（Executor / ParameterHandler / ResultSetHandler / StatementHandler）的拦截方式。

### 9. 自定义 TypeHandler

`type/TagsTypeHandler`：数据库里 `tags` 是 `["玄幻","热血"]` 这样的 JSON 数组文本，
Java 侧直接映射成 `List<String>`，用在 `NovelMapper.xml` 的 resultMap 与 insert/update 里。

### 10. 其它细节

- **主键回填**：`@Options(useGeneratedKeys = true, keyProperty = "id")`
- **批量插入**：`<foreach>` 一条 INSERT 写多行（21 章一次入库）
- **Upsert**：`HistoryMapper.xml` 的 `INSERT ... ON DUPLICATE KEY UPDATE`
- **原子自增**：`UPDATE novel SET view_count = view_count + 1`（避免并发丢更新）
- **乐观 CAS**：`DownloadTaskMapper.updateStatusCas`（`WHERE id=? AND status=?`）
- **#{} 与 ${}**：项目全部用 `#{}` 预编译；唯一需要拼接的排序字段用 `<choose>` 白名单规避

---

## 六、数据库表

| 表名 | 说明 | 关键字段 |
|---|---|---|
| `sys_user` | 用户 | username(唯一)、role(ADMIN/USER)、status、deleted |
| `category` | 分类 | name(唯一)、sort_num |
| `novel` | 小说 | category_id、**tags**(TypeHandler)、word_count、chapter_count、status、view/favorite/download_count、file_path |
| `chapter` | 章节 | novel_id、chapter_no(联合唯一)、content(MEDIUMTEXT) |
| `favorite` | 收藏 | user_id + novel_id 唯一 |
| `reading_history` | 阅读历史/书架 | user_id + novel_id 唯一、chapter_no、progress |
| `download_task` | 下载任务 | task_no(唯一)、status、total_chapter、done_chapter |
| `sys_log` | 操作日志 | module、operation、cost_ms |

下载任务状态机：

```
DOWNLOADING --暂停--> PAUSED --继续--> DOWNLOADING
     |                  |
     +-----停止---------+--> CANCELLED
     |
     +--> 全部章节下载完 --> COMPLETED（同时小说下载数 +1）
```

---

## 七、目录结构

```
src/main/java/org/example/mybatis/
├── MybatisApplication.java          @MapperScan
├── common/                          Result / PageResult / BizException / GlobalExceptionHandler / SessionUtil
├── config/MyBatisConfig.java        注册 MyBatis 插件
├── plugin/                          PageContext / PageInterceptor（分页）/ SlowSqlInterceptor（慢SQL）
├── type/TagsTypeHandler.java        自定义类型转换器
├── runner/DownloadRunner.java       下载任务异步执行器（状态机 + 线程池）
├── entity/                          7 个实体
├── dto/                             查询条件 + VO
├── mapper/                          8 个 Mapper 接口（注解 + XML 混合）
├── service/ + service/impl/         7 组服务
└── controller/                      9 个控制器

src/main/resources/
├── application.yaml                 数据源 / MyBatis / 上传配置
├── db/schema.sql、db/data.sql        建表脚本 + 演示数据
├── mapper/*.xml                     8 个 XML 映射文件（复杂 SQL）
└── static/                          login / index / reader / downloads / admin + css + js
```

---

## 八、主要接口

统一返回 `{ code, msg, data }`，`code=0` 为成功。

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/auth/login` | 登录，返回 user 与 home 页 | 公开 |
| POST | `/api/auth/register` | 注册（普通用户） | 公开 |
| GET | `/api/auth/info` | 当前登录用户 | 公开 |
| GET | `/api/categories` | 分类列表（走二级缓存） | 公开 |
| GET | `/api/categories/with-novels` | 分类 + 旗下小说（`<collection>`） | 公开 |
| GET | `/api/novels` | 小说检索：keyword / categoryId / status / tags / 字数区间 / orderBy + 分页 | 公开 |
| GET | `/api/novels/{id}` | 详情（`<association>` + 浏览数自增） | 公开 |
| GET | `/api/novels/hot` | 热门 TOP N | 公开 |
| POST | `/api/novels/upload` | 上传 TXT 自动分章 | 管理员 |
| GET | `/api/novels/stat/*` | overview / category / author / daily-novel / daily-download / user-rank | 管理员 |
| GET | `/api/chapters/catalog/{novelId}` | 目录 | 公开 |
| GET | `/api/chapters/{id}` | 章节正文 + 上下章 | 公开 |
| POST | `/api/favorites/toggle/{novelId}` | 收藏/取消（事务 + 计数联动） | 登录 |
| GET | `/api/favorites` | 我的收藏（多表 JOIN） | 登录 |
| POST | `/api/history` | 记录阅读进度（upsert） | 登录 |
| GET | `/api/history` | 我的书架 | 登录 |
| POST | `/api/downloads/create/{novelId}` | 创建并开始下载 | 登录 |
| PUT | `/api/downloads/{id}/pause` | 暂停 | 本人 |
| PUT | `/api/downloads/{id}/resume` | 继续 | 本人 |
| PUT | `/api/downloads/{id}/cancel` | 停止 | 本人 |
| PUT | `/api/downloads/cancel-all` | 全部停止（批量 foreach） | 登录 |
| GET | `/api/users` | 用户分页查询 | 管理员 |

---

## 九、踩过的坑（很有参考价值）

### 1. MyBatis Starter 版本必须匹配 Spring Boot 4

本地缓存的 `mybatis-spring-boot-starter 3.0.4` 是基于 Spring Framework 6 的。
Spring Boot 4.1.1 用的是 Spring Framework 7，必须升级到 **4.0.1**，否则启动就失败。

### 2. JDBC URL 里不能写 `characterEncoding=utf8mb4`

会报 `UnsupportedEncodingException: utf8mb4`。
`utf8mb4` 是 MySQL 的字符集名，不是 Java 的编码名；Connector/J 8+ 默认就是 utf8mb4，直接去掉该参数即可。

### 3. 分页插件的 COUNT 查询不能用原 resultMap

一开始把 COUNT 的 `MappedStatement` 直接复制自原语句，沿用了 `NovelVOMap`，
导致结果被映射成 `null` → `Number.longValue()` 报 NPE。

解决：为 COUNT 单独构造一个 `resultType=Long` 的 ResultMap。

### 4. 开启二级缓存后，查询结果类必须实现 Serializable

`NovelMapper` 开了 `<cache>`，`NovelDetailVO` 没实现 `Serializable` → `NotSerializableException`。
现在 `NovelVO` / `NovelDetailVO` / `FavoriteVO` / `HistoryVO` / `DownloadTaskVO` 均已实现。

### 5. 实时计数不能走二级缓存

详情页要实时反映浏览数和收藏数，但最初被缓存住了，导致收藏后数字不更新。
解决：`selectDetail` 加 `useCache="false"`；同时给高频的「浏览数 +1」加
`@Options(flushCache = FlushCachePolicy.FALSE)`，避免每次浏览都清空整个命名空间的缓存。

### 6. Maven：`test-compile` 与 `testCompile`

Spring Boot 初始模板生成的 `pom.xml` 里 goal 写成 Maven 4 风格的 `test-compile`，
在本机 Maven 3.9 下会报 `Could not find goal 'test-compile'`，需改成 `testCompile`。

---

## 十、如何直观感受各个知识点

| 想看什么 | 怎么做 |
|---|---|
| 执行的 SQL | 控制台会实时打印（`log-impl: StdOutImpl`） |
| **分页插件** | 书城翻页，观察日志里自动追加的 `LIMIT` 和前面的 `SELECT COUNT(*)` |
| **二级缓存命中** | 多次刷新「分类管理」，观察日志里的 `Cache Hit Ratio [...CategoryMapper]` |
| **事务回滚** | 上传一个超大/空文件，观察小说和章节都没入库 |
| **TypeHandler** | 小说详情里的 tags 在库里是 JSON 文本，接口里是数组 |
| **慢 SQL 插件** | 调大查询范围，超过 `novel.slow-sql-threshold-ms` 会打印【慢SQL】告警 |
| **下载状态机** | 上传一个多章小说 → 创建下载任务 → 暂停/继续/停止，看进度是否冻结/推进 |
| **upsert** | 反复阅读同一章，观察阅读历史记录数不增加、进度在更新 |
