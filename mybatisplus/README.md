# 单车商城（Spring Boot + MyBatis-Plus）

一个基于 Spring Boot 4 + MyBatis-Plus 的自行车售卖网站示例，包含**普通用户（卖场下单）**与**后台管理员（商品/用户/订单管理）**两种角色，前端为三个纯静态页面，全部功能通过 REST 接口 + MyBatis-Plus CRUD 实现。

---

## 一、技术栈

| 组件 | 版本 | 说明 |
|---|---|---|
| JDK | 21 | `java.version` = 21 |
| Spring Boot | 4.1.1 | `spring-boot-starter-webmvc` |
| MyBatis-Plus | 3.5.17 | `mybatis-plus-spring-boot4-starter` |
| MySQL | 8.x | `mysql-connector-j`（runtime） |
| Maven | 3.9.16 | 通过 `mvnw` 使用 |
| 前端 | 原生 HTML/CSS/JS | 无框架，放在 `static/` |

> MyBatis-Plus 3.5.17 额外需要 `mybatis-plus-jsqlparser` 依赖，分页插件 `PaginationInnerInterceptor` 已拆分到该模块中。

---

## 二、快速开始

### 1. 准备数据库

只要本机 MySQL 已启动即可，**无需手工执行任何 SQL**。应用启动时会自动完成：

1. 数据库不存在时自动创建 `bike_shop`（连接串带 `createDatabaseIfNotExist=true`）
2. 执行 `src/main/resources/db/schema.sql` 建三张表（`CREATE TABLE IF NOT EXISTS`）
3. 执行 `src/main/resources/db/data.sql` 插入演示数据（`INSERT IGNORE`，重复启动不会覆盖已有数据）

默认连接配置（`src/main/resources/application.yaml`）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/bike_shop?createDatabaseIfNotExist=true&...
    username: root
    password: '412826'
```

如需修改库名/账号/密码，改这一处即可。

### 2. 启动

```powershell
cd d:\daima\Lianshi\springboot-xuexi\mybatisplus
.\mvnw.cmd spring-boot:run
```

或打包后运行：

```powershell
.\mvnw.cmd -DskipTests package
java -jar target/mybatisplus-0.0.1-SNAPSHOT.jar
```

默认端口 **8080**。若被占用：

```powershell
java -jar target/mybatisplus-0.0.1-SNAPSHOT.jar --server.port=8081
```

### 3. 访问

浏览器打开 <http://localhost:8080>

---

## 三、页面与账号

| 页面 | 路径 | 访问条件 |
|---|---|---|
| 根路径 | `/` | 302 重定向到登录页 |
| 登录 / 注册页 | `/login.html` | 所有人 |
| 自行车售卖页 | `/index.html` | 已登录用户（未登录自动跳登录页） |
| 后台管理页 | `/admin.html` | 仅管理员（非管理员弹窗后跳回卖场） |
| **MyBatis-Plus 实验室** | `/lab.html` | 所有人（每个按钮对应一个 MP 特性） |
| **批量上架商品** | `/batch.html` | 管理员（insertBatchSomeColumn 批量写入） |

演示账号：

| 账号 | 密码 | 角色 | 登录后跳转 |
|---|---|---|---|
| `admin` | `123456` | 管理员 ADMIN | `/admin.html` |
| `user` | `123456` | 普通用户 USER | `/index.html` |

注册页面只能注册**普通用户**，管理员只能由后台「用户管理」创建。

> 页面必须通过 `http://localhost:8080` 访问，不能直接双击打开 HTML 文件——接口调用都是同源相对路径 `/api/...`。

---

## 四、功能说明

### 卖场页面（`/index.html`）

- 关键字搜索（车名 / 品牌 / 分类）+ 分类筛选 + 分页
- 商品卡片展示（图片地址为空时用分类图标兜底）
- 点击「立即购买」弹出下单框，自动带出昵称/电话/地址
- 「我的订单」查看订单、取消订单（取消后库存归还）
- 库存为 0 或商品下架时按钮置灰

### 后台管理页面（`/admin.html`）

三个 Tab，均为完整 CRUD：

- **自行车管理**：新增 / 编辑 / 删除（逻辑删除）、按关键字与分类查询、上架下架
- **用户管理**：新增 / 编辑 / 删除用户、可指定角色（管理员 / 普通用户）、不能删除当前登录的管理员
- **订单管理**：查询全部订单（按状态筛选）、发货 → 完成、取消订单（归还库存）

顶部三个统计卡片分别显示商品数、用户数、订单数。

---

## 五、数据库表

| 表名 | 说明 | 主要字段 |
|---|---|---|
| `t_user` | 用户（普通用户 + 管理员） | `username`(唯一)、`password`、`nickname`、`phone`、`address`、`role`(`USER`/`ADMIN`)、`create_time`、`deleted` |
| `t_bike` | 自行车商品 | `name`(唯一)、`brand`、`category`、`price`、`stock`、`status`(1 在售 / 0 下架)、`image_url`、`description`、`create_time`、`update_time`、`deleted` |
| `t_order` | 订单 | `order_no`(唯一)、`user_id`、`username`、`bike_id`、`bike_name`、`price`、`quantity`、`total_amount`、`receiver`、`phone`、`address`、`status`、`create_time`、`deleted` |

- 三张表均使用 `deleted` 字段做**逻辑删除**（MyBatis-Plus `@TableLogic`）
- 销量/销售额由 `t_bike` LEFT JOIN `t_order` 的子查询现算，不冗余存储
- 订单状态取值：`待发货` / `已发货` / `已完成` / `已取消`
- 商品分类取值：`山地车` / `公路车` / `折叠车` / `城市车` / `儿童车`

---

## 六、接口清单

统一返回结构：

```json
{ "code": 0, "msg": "success", "data": {} }
```

`code = 0` 为成功，非 0 时 `msg` 为错误原因，由全局异常处理器统一返回。

### 认证 `/api/auth`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/auth/login` | 登录，返回 `user` 与 `home`（待跳转页面） | 公开 |
| POST | `/api/auth/register` | 注册（固定为普通用户） | 公开 |
| GET | `/api/auth/info` | 当前登录用户，未登录返回 `null` | 公开 |
| POST | `/api/auth/logout` | 退出登录，销毁 Session | 登录 |

### 自行车 `/api/bikes`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/bikes` | 卖场分页查询（只返回在售） | 公开 |
| GET | `/api/bikes/admin` | 后台分页查询（含下架） | 管理员 |
| **GET** | **`/api/bikes/search`** | **动态 SQL 高级检索（含销量）** | 公开（非管理员强制只看在售） |
| **GET** | **`/api/bikes/stat`** | **动态 SQL 分类聚合统计** | 管理员 |
| **GET** | **`/api/bikes/options`** | **检索选项：分类 / 品牌去重列表** | 公开 |
| GET | `/api/bikes/{id}` | 商品详情 | 公开 |
| POST | `/api/bikes` | 新增商品 | 管理员 |
| PUT | `/api/bikes/{id}` | 修改商品 | 管理员 |
| DELETE | `/api/bikes/{id}` | 删除商品（逻辑删除） | 管理员 |

查询参数：`keyword`、`category`、`current`（默认 1）、`size`（卖场默认 8 / 后台默认 10）

### 用户 `/api/users`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/users` | 用户分页列表 | 管理员 |
| POST | `/api/users` | 新增用户（可指定角色） | 管理员 |
| PUT | `/api/users/{id}` | 修改用户（密码留空表示不改） | 管理员 |
| DELETE | `/api/users/{id}` | 删除用户 | 管理员 |

查询参数：`keyword`、`role`、`current`、`size`

### 订单 `/api/orders`

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/orders` | 下单（校验并扣减库存），`?lock=atomic`（默认）/ `pessimistic`（悲观锁）/ `optimistic`（乐观锁） | 登录用户 |
| GET | `/api/orders/my` | 我的订单（`user_id` 条件由数据权限插件自动拼） | 登录用户 |
| GET | `/api/orders` | 全部订单 | 管理员 |
| GET | `/api/orders/stat` | 订单状态统计（QueryWrapper 的 select + groupBy） | 管理员 |
| PUT | `/api/orders/{id}/status?status=xxx` | 修改订单状态（传 WAIT/SENT/DONE/CANCEL 或中文都行） | 管理员 |
| DELETE | `/api/orders/{id}` | 取消订单（归还库存） | 本人或管理员 |
| POST | `/api/bikes/batch` | 批量上架商品（insertBatchSomeColumn） | 管理员 |

订单状态是枚举：`WAIT / SENT / DONE / CANCEL`，数据库存中文（待发货/已发货/已完成/已取消），
JSON 里返回枚举名，前端自己映射成中文（见 `index.html` / `admin.html` 里的 `STATUS_TEXT`）。

### MyBatis-Plus 实验室 `/api/lab`

| 方法 | 路径 | 演示的特性 |
|---|---|---|
| GET | `/api/lab/plugins` | 当前生效的插件清单 |
| POST | `/api/lab/fill` | 自动填充 MetaObjectHandler |
| POST | `/api/lab/enum` | 枚举 @EnumValue / IEnum |
| GET | `/api/lab/data-permission?userId=2` | 行级数据权限 |
| POST | `/api/lab/tenant?name=demo` | 多租户 TenantLine |
| POST | `/api/lab/dynamic-table?table=t_order_2026` | 动态表名（订单归档） |
| POST | `/api/lab/concurrent?bikeId=1&threads=20&stock=10&mode=atomic` | 三种并发控制压测 |
| GET | `/api/lab/batch?n=1000` | 三种批量插入耗时对比 |
| GET | `/api/lab/block-attack` | 拦截全表更新/删除 |
| GET | `/api/lab/max-limit?size=10000` | 分页 maxLimit |

下单请求体：

```json
{ "bikeId": 1, "quantity": 2, "receiver": "张三", "phone": "13900000001", "address": "北京市朝阳区xx路1号" }
```

业务规则：
- 下架商品不能下单；库存不足时下单失败
- 扣库存用 `setSql("stock = stock - N")` + `ge(stock, N)` 保证原子性
- 取消订单会把库存加回；已完成 / 已取消的订单不能重复取消

---

## 七、动态 SQL（XML 与注解两种写法）

同一个需求做了两套等价实现，用来对比 MyBatis 动态 SQL 的两种写法：

**需求**：商品高级检索 + 分类销售统计

- 条件全部可选：关键字、分类多选、品牌多选、价格区间、最低库存、最低销量、上下架状态
- 关联 `t_order` 子查询算出每款车的**累计销量 / 销售额**（排除已取消订单）
- 动态排序（白名单枚举，杜绝 SQL 注入）
- 统计按 `category` 做 `GROUP BY` 聚合

### 实现一：Mapper XML

文件：`src/main/resources/mapper/BikeMapper.xml`（需在 `application.yaml` 配 `mapper-locations`）

- `searchWithXml` — 分页检索
- `statWithXml` — 分组统计

用到的动态标签：`<sql>` + `<include>`（条件片段复用）、`<where>`、`<if>`、`<foreach>`、`<choose>/<when>/<otherwise>`

### 实现二：接口注解

文件：`src/main/java/.../mapper/BikeMapper.java`

- `searchWithAnnotation` — SQL 写在 `@Select({"<script>", ... , "</script>"})` 里
- `statWithAnnotation` — 同上

注解里同样支持全部动态标签，区别是**无法用 `<sql>` 片段复用**，条件要写两遍。

### 两种写法对比

| | Mapper XML | 接口注解 |
|---|---|---|
| SQL 位置 | 独立 `.xml` 文件 | Java 方法上的 `@Select` |
| 复杂 SQL 可读性 | 好，有缩进和语法高亮 | 一般，字符串拼接 |
| 片段复用 | 支持 `<sql>` + `<include>` | 不支持 |
| 改 SQL 是否需重编译 | 否（IDE 支持热加载） | 是 |
| 适用场景 | 长 SQL、多表关联、统计报表 | 短 SQL、简单查询 |

### 切换方式

接口加 `impl` 参数即可切换，两种实现返回结果完全一致（已实测对比）：

```
GET /api/bikes/search?impl=xml         # Mapper XML 实现（默认）
GET /api/bikes/search?impl=annotation  # 接口注解实现
GET /api/bikes/stat?impl=xml
GET /api/bikes/stat?impl=annotation
```

### 请求示例

```
# 分类多选 + 价格区间 + 最低销量 + 按销量倒序
GET /api/bikes/search?categories=山地车,公路车&minPrice=1000&maxPrice=4000&minSold=1&orderBy=sold_desc

# 关键字 + 按价格升序
GET /api/bikes/search?keyword=捷安特&orderBy=price_asc
```

可用排序值：`price_asc` / `price_desc` / `stock_asc` / `stock_desc` / `sold_desc` / `id_desc`

### 防止 SQL 注入

排序字段没有直接拼接 `${}` 参数，而是用 `<choose>` 枚举白名单：

```xml
<choose>
    <when test="q.orderBy == 'price_asc'">ORDER BY b.price ASC</when>
    <when test="q.orderBy == 'sold_desc'">ORDER BY sold_count DESC</when>
    <otherwise>ORDER BY b.id DESC</otherwise>
</choose>
```

所有用户输入值一律用 `#{}` 预编译占位符。

---

## 八、并发控制：悲观锁 vs 原子 UPDATE

下单扣库存是典型的并发场景。项目里实现了**两种**方案，可通过参数切换对比。

### 方案一：原子 UPDATE（默认，无锁）

```java
// 靠 WHERE stock >= quantity 保证不会超卖，更新行数为 0 即库存不足
bikeMapper.update(null, Wrappers.<Bike>lambdaUpdate()
        .eq(Bike::getId, bike.getId())
        .ge(Bike::getStock, quantity)
        .setSql("stock = stock - " + quantity));
```

特点：**不加锁**，并发时失败的一方立即返回（快速失败），吞吐量高。

### 方案二：悲观锁（SELECT ... FOR UPDATE）

```java
// 1) 加行级排他锁（必须在事务内，否则锁立即释放、等于没加）
@Select("SELECT * FROM t_bike WHERE id = #{id} AND deleted = 0 FOR UPDATE")
Bike selectByIdForUpdate(@Param("id") Long id);

// 2) 持锁后读-改-写，此刻是安全的
bike.setStock(bike.getStock() - quantity);
bikeMapper.updateById(bike);
```

特点：**串行化**，其它事务在该行上阻塞等待，直到本事务提交/回滚。适合扣减后还有复杂业务逻辑、或必须先读后写的场景。

> **关键点**：`FOR UPDATE` 只在事务内有效。本项目在 `BikeOrderServiceImpl.createOrder` 上加 `@Transactional(rollbackFor = Exception.class)`，且锁住商品行后仍在**同一事务**内完成扣减与订单插入。

### 切换方式

```
POST /api/orders?lock=atomic        # 原子 UPDATE（默认）
POST /api/orders?lock=pessimistic   # 悲观锁 FOR UPDATE
```

前端下单弹窗也有「库存扣减方式」下拉，可实时切换。

### 实测对比（库存 3，并发 8 个请求各买 1 件）

| 方式 | 成功 | 失败 | 各请求耗时 |
|---|---|---|---|
| 原子 UPDATE | 3 | 5 | 47 / 48 / 51 ms（全部并行，无阻塞） |
| 悲观锁（持锁演示延迟 400ms） | 3 | 5 | **457 → 875 → 1299** ms（阶梯递增，明显串行排队） |

两者都**没有超卖**，区别在于并发行为：
- 原子 UPDATE：并行执行，抢不到库存的立即失败
- 悲观锁：排队执行，耗时随并发数线性增长

### 自行验证

`application.yaml` 里有个演示开关，用来放大并发窗口、观察锁的串行化：

```yaml
bike:
  order:
    lock-demo-delay-ms: 0   # 默认 0（不暂停）；想看效果设成 400~500
```

也可以启动时不改配置：

```powershell
java -jar target/mybatisplus-0.0.1-SNAPSHOT.jar --bike.order.lock-demo-delay-ms=400
```

然后把某商品库存改成 3，用浏览器/脚本同时发 8 个下单请求，观察耗时阶梯。

> 生产环境务必把 `lock-demo-delay-ms` 保持为 0。

### 两种方案怎么选

| 场景 | 建议 |
|---|---|
| 单纯的库存扣减 | 原子 UPDATE（性能更好） |
| 扣减后还有复杂计算/多次写 | 悲观锁（逻辑更直观、不易出错） |
| 高并发秒杀 | 原子 UPDATE / Redis 预扣，悲观锁会成为瓶颈 |

---

## 九、目录结构

```
src/main/java/org/example/mybatisplus/
├── MybatisplusApplication.java       启动类（@MapperScan）
├── common/
│   ├── Result.java                   统一返回体
│   ├── BizException.java             业务异常
│   ├── GlobalExceptionHandler.java   全局异常处理
│   └── SessionUtil.java              登录会话 / requireLogin / requireAdmin
├── config/MybatisPlusConfig.java     分页插件
├── controller/
│   ├── PageController.java           "/" -> redirect:/login.html
│   ├── AuthController.java           登录注册退出
│   ├── BikeController.java           商品 CRUD
│   ├── UserController.java           用户 CRUD
│   └── OrderController.java          订单
├── dto/                              LoginRequest / RegisterRequest / OrderRequest
├── entity/                           User / Bike / BikeOrder
├── mapper/                           UserMapper / BikeMapper / BikeOrderMapper
└── service/ + service/impl/          业务层（继承 MyBatis-Plus IService / ServiceImpl）

src/main/resources/
├── application.yaml                  数据源、MyBatis-Plus、SQL 初始化配置
├── db/schema.sql                     建表脚本
├── db/data.sql                       演示数据
├── mapper/BikeMapper.xml             动态 SQL（XML 实现）
└── static/
    ├── login.html                    登录 / 注册
    ├── index.html                    自行车售卖页
    ├── admin.html                    后台管理页
    ├── css/common.css                公共样式
    └── js/common.js                  fetch 封装、提示、分页
```

---

## 十、常见问题

### 1. 登录报 `POST /api/auth/login 404`

说明 8080 端口上运行的是**改动前的旧实例**（例如改动代码前在 IDEA 里启动的进程）。静态页面会从 `target/classes` 重新加载看起来是新的，但 Controller 不会被重新注册。

解决：在 IDEA 里 Stop 掉旧进程后重新 Run，或 `Build → Rebuild Project` 再启动。

### 2. `Could not find goal 'test-compile' in plugin maven-compiler-plugin`

Spring Boot 4.1.1 初始模板生成的 `pom.xml` 使用了 Maven 4 风格的目标名。已修正为 Maven 3.9 兼容的写法：

```xml
<execution>
    <id>default-testCompile</id>
    <phase>test-compile</phase>
    <goals>
        <goal>testCompile</goal>   <!-- 原为 test-compile -->
    </goals>
</execution>
```

### 3. MyBatis-Plus 3.5.17 包名找不到 `IService` / `ServiceImpl`

3.5.17 调整了包结构：

```java
import com.baomidou.mybatisplus.spring.service.IService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
```

分页插件 `PaginationInnerInterceptor` 位于 `com.baomidou.mybatisplus.extension.plugins.inner`，需额外引入 `mybatis-plus-jsqlparser` 依赖。

### 4. 想重置演示数据

删除 `bike_shop` 数据库后重启应用即可自动重建；或只删某几行，重启时 `INSERT IGNORE` 不会重新插入同名记录。

---

## 十一、MyBatis-Plus 实验室（插件与高级特性）

访问 <http://localhost:8080/lab.html>，九个卡片对应九个特性，点按钮就能看到真实效果。

### 1. 特性清单

| 特性 | 落地位置 | 演示入口 |
|---|---|---|
| 分页 `PaginationInnerInterceptor`（maxLimit=200） | `config/MybatisPlusConfig` | 实验室 ⑧ |
| 防全表更新/删除 `BlockAttackInnerInterceptor` | 同上 | 实验室 ⑧（故意不带 where） |
| 乐观锁 `@Version` + `OptimisticLockerInnerInterceptor` | `entity/Bike.version`、`service/BikeStockService` | 实验室 ①、下单 `?lock=optimistic` |
| 多租户 `TenantLineInnerInterceptor` | `handler/DemoTenantLineHandler`（只对 `t_tenant_goods` 生效） | 实验室 ⑤ |
| 动态表名 `DynamicTableNameInnerInterceptor` | `handler/OrderTableHandler`（只对 `t_order` 生效） | 实验室 ⑥ |
| 数据权限 `DataPermissionInterceptor` | `handler/OrderDataPermissionHandler` | 实验室 ④、`/api/orders/my` |
| 自动填充 `MetaObjectHandler` | `handler/MyMetaObjectHandler` | 实验室 ② |
| 枚举 `@EnumValue` / `IEnum` | `enums/OrderStatus` | 实验室 ③、订单状态 |
| 批量插入增强 `InsertBatchSomeColumn` | `injector/MySqlInjector` + `mapper/EasyBaseMapper` | 实验室 ⑦、`/batch.html` |
| 代码生成器 | `tools/CodeGenerator.java`（不参与编译） | 实验室 ⑨ 有运行说明 |

### 2. 三种并发控制实测（库存 10，20 个线程各买 1 件）

| 方式 | 成功 | 失败 | 剩余库存 | 耗时 |
|---|---|---|---|---|
| atomic 原子 UPDATE | 10 | 10 | 0 | ~150 ms |
| pessimistic 悲观锁 FOR UPDATE | 10 | 10 | 0 | ~190 ms |
| optimistic 乐观锁 @Version | 10 | 10 | 0 | ~800 ms |

结论：三种都**不会超卖**；原子 UPDATE 最快，悲观锁次之，乐观锁因为要反复重试最慢，
但它是唯一「不加数据库锁」的方案，适合冲突少的场景。

### 3. 批量插入实测（1000 条）

| 写法 | 耗时 |
|---|---|
| 逐条 `insert` | 2263 ms |
| `saveBatch`（JDBC Batch） | 199 ms |
| `insertBatchSomeColumn`（一条 SQL 多组 VALUES） | 114 ms |

前提：JDBC URL 上加了 `rewriteBatchedStatements=true`，否则 `saveBatch` 只是「看起来批量」。

### 4. 踩坑记录

| 现象 | 原因 | 解决 |
|---|---|---|
| 乐观锁重试 10 次仍然几乎全部失败 | MySQL 可重复读：同一事务里 select 读的是旧快照，version 永远冲突 | 把「读 + 带 version 更新」放进 `BikeStockService`，用 `REQUIRES_NEW` 每次重试开新事务 |
| 压测报 `Could not open JDBC Connection` | REQUIRES_NEW 每次重试都要新连接，20 并发把默认 10 条连接池打爆 | `hikari.maximum-pool-size: 60` |
| 批量插入报 `Column 'deleted' cannot be null` | `insertBatchSomeColumn` 不会注入逻辑删除/版本号的默认值 | 实体里给 `deleted = 0`、`version = 0` 初始值 |
| 清空压测表报 `Prohibition of full table deletion` | 这正是 `BlockAttackInnerInterceptor` 在起作用 | 清空改成带条件：`delete(Wrappers.lambdaQuery().ge(BatchDemo::getId, 0L))` |
| 枚举存成了枚举名（WAIT） | 没配默认枚举处理器 | `mybatis-plus.configuration.default-enum-type-handler=com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler` |
| `DynamicTableNameInnerInterceptor(Map)` 编译不过 | 3.5.17 的构造器只收**单个** `TableNameHandler` | 在 handler 里自己判断表名，其它表原样返回 |
| `MySqlInjector` 覆写 `getMethodList(Class)` 报错 | 3.5.17 的签名是 `(Configuration, Class, TableInfo)` | 按三参数覆写 |

### 5. 新增/改动的文件

```text
config/MybatisPlusConfig.java        插件链 + SqlInjector
handler/MyMetaObjectHandler.java     自动填充
handler/OrderDataPermissionHandler.java   数据权限
handler/DemoTenantLineHandler.java   多租户
handler/OrderTableHandler.java       动态表名
holder/DataScopeContext.java         数据权限上下文（ThreadLocal）
holder/TenantContext.java            租户上下文
holder/DynamicTableContext.java      动态表名上下文
injector/MySqlInjector.java          注入 insertBatchSomeColumn
mapper/EasyBaseMapper.java           增强 BaseMapper
enums/OrderStatus.java               订单状态枚举
enums/LockMode.java                  并发控制方式
service/BikeStockService.java        乐观锁扣库存（REQUIRES_NEW）
service/LabService.java              实验室业务
controller/LabController.java        实验室接口
entity/TenantGoods.java / BatchDemo.java   演示实体
static/lab.html + js/lab.js          MP 实验室页面
static/batch.html + js/batch.js      批量上架页面
tools/CodeGenerator.java             代码生成器（独立）
db/schema.sql                        幂等加列 + 3 张演示表
```

> ThreadLocal 用完必须 `clear()`：数据权限、租户、动态表名三个上下文都在 `finally` 里清理，
> 否则线程池复用会把条件串到下一个请求上。
