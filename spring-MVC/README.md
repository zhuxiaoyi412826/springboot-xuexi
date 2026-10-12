# Spring MVC 实验室（spring-MVC）

一个**只为学习 Spring MVC 而生**的最小 Web 项目：没有数据库、没有模板引擎，
前端是 `src/main/resources/static` 下的原生 HTML/CSS/JS，后端按知识域拆分成十几个 Controller，
把 Spring MVC 的常见知识点**逐个做成可点击、可观察**的接口。

---

## 一、技术栈与版本

| 组件 | 版本 | 说明 |
| --- | --- | --- |
| JDK | 21 | |
| Spring Boot | 4.1.1 | 注意：Boot 4 里 web starter 改名为 `spring-boot-starter-webmvc` |
| Spring Framework | 7.0.9 | 随 Boot 4.1.1 |
| Jackson | 3.1.5 | **Jackson 3 的包名是 `tools.jackson.*`**（注解仍在 `com.fasterxml.jackson.*`） |
| Servlet 容器 | Tomcat 11.0.24 | Jakarta EE 11（`jakarta.servlet.*`） |
| 校验 | hibernate-validator 9.1.3 | Jakarta Validation 3.1 |
| 构建 | Maven 3.9.9 | 端口 **8083** |

---

## 二、快速开始

```bash
# 方式一：Maven 直接跑
cd d:\daima\Lianshi\springboot-xuexi\spring-MVC
mvn spring-boot:run

# 方式二：打 jar 后运行
mvn package -DskipTests
java -jar target/spring-mvc-0.0.1-SNAPSHOT.jar
```

打开 <http://localhost:8083> 即可看到实验清单首页；
想看后端注册了多少映射，访问 <http://localhost:8083/mvc/monitor/mappings>。

> 单元测试：`mvn test`（9 个用例：7 个 MockMvc standalone 用例 + 2 个完整容器启动用例）。

---

## 三、页面清单（全部在 `static` 下）

| 页面 | 对应知识域 |
| --- | --- |
| `index.html` | 实验清单 + 快捷调试 |
| `pages/hello.html` | ①返回值与视图（视图名 / ModelAndView / forward / redirect / void / @ResponseStatus / 自定义 ViewResolver） |
| `pages/param.html` | ②参数绑定（@RequestParam / @PathVariable / @RequestHeader / @CookieValue / @RequestAttribute / @ModelAttribute / POJO 嵌套 / 集合） |
| `pages/convert.html` | ③类型转换与格式化（Converter / Formatter / PropertyEditor / @DateTimeFormat / @NumberFormat / ConversionService） |
| `pages/validate.html` | ④数据校验（@Valid / @Validated 分组 / 自定义约束 / BindingResult / Spring Validator） |
| `pages/rest.html` | ⑤REST（@RestController / ResponseEntity / HttpEntity / @JsonView / consumes·produces） |
| `pages/async.html` | ⑥异步请求（Callable / WebAsyncTask / DeferredResult / Emitter / SSE / StreamingResponseBody） |
| `pages/file.html` | ⑦文件上传下载（MultipartFile / 多文件 / @RequestPart+JSON / Part / 三种下载方式） |
| `pages/session.html` | ⑧会话（HttpSession / @SessionAttributes / FlashMap / Cookie / 登录拦截器） |
| `pages/i18n.html` | ⑨国际化（MessageSource / LocaleResolver / LocaleChangeInterceptor） |
| `pages/exception.html` | ⑩异常处理（@ControllerAdvice / 自定义解析器 / ResponseStatusException / ProblemDetail） |
| `pages/advanced.html` | ⑪扩展点（自定义参数解析器 / 返回值处理器 / HttpMessageConverter / 内容协商） |
| `pages/path.html` | ⑫路径匹配与 CORS（Ant 通配 / 正则 / params·headers·consumes / 跨域） |
| `pages/monitor.html` | ⑬运行时观察（HandlerMapping 全量映射 / 监听器 / 过滤器痕迹 / 组件速查表） |

---

## 四、知识点地图（知识点 → 实现位置 → 怎么观察）

| # | 知识点 | 实现位置 | 观察方式 |
| --- | --- | --- | --- |
| 1 | DispatcherServlet 前端控制器 | Boot 自动注册（`/`） | 任意请求；`/my-servlet` 是不走它的原生 Servlet |
| 2 | `@RequestMapping` 全家桶 | 所有 Controller | `/mvc/monitor/mappings`（共 98 条） |
| 3 | 路径变量/正则/通配符 | `PathController` | `/mvc/path/ant/a`、`/mvc/path/regex/123` |
| 4 | 映射条件 params/headers/consumes/produces | `PathController` | `/mvc/path/condition?type=json` |
| 5 | 参数绑定注解 | `BindController` | `/mvc/bind/header`、`/mvc/bind/cookie`、`/mvc/bind/attr` |
| 6 | POJO / 嵌套 / 集合绑定 | `BindController#pojo` | `?address.province=GD&tags=a&tags=b` |
| 7 | `@InitBinder` 白名单 | `BindController#initBinder` | 提交 `id=999` 被忽略 |
| 8 | 自定义 Converter | `StringToDeptConverter` + `WebMvcConfig#addFormatters` | `/mvc/convert/dept?dept=10:RD` |
| 9 | 自定义 Formatter | `MoneyFormatter` | `/mvc/convert/money?money=1,234.56` |
| 10 | PropertyEditor | `DatePropertyEditor` + 全局 `@InitBinder` | `User.vipExpire`（`/mvc/bind/pojo?vipExpire=2030-12-31`） |
| 11 | `@DateTimeFormat` / `@NumberFormat` | `BindController#date` | `/mvc/bind/date?date=2026-01-01` |
| 12 | JSR-380 校验 | `User` 字段注解 | POST `/mvc/validate/body` |
| 13 | 校验分组 | `ValidationGroups` | POST `/mvc/validate/create` vs `/update` |
| 14 | 自定义约束注解 | `@Phone` + `PhoneValidator` | 传 `phone=123` |
| 15 | Spring `Validator` 接口 | `UserValidator` + `binder.setValidator` | `/mvc/validate/spring-validator` |
| 16 | `BindingResult` 手工处理 | `BindController#modelAttr`、`ValidateController#form` | 返回 400 + 字段错误 |
| 17 | 返回值类型 | `HelloController` | `/mvc/hello/view`、`/mav`、`/forward`、`/redirect`、`/void`、`/created` |
| 18 | 自定义 View / ViewResolver | `StaticView`、`StaticViewResolver` | 视图名 `static:hello` → `/pages/hello.html` |
| 19 | `RedirectAttributes` + FlashMap | `HelloController#redirect`、`SessionCookieController#login` | 重定向后 `/mvc/session/me` 里的 `flashMsg` |
| 20 | `@SessionAttributes` | `WizardController` | `/mvc/wizard/step?key=name&value=a` → `/finish` |
| 21 | `@ModelAttribute`（方法级/全局） | `BindController#serverTime`、`GlobalModelAdvice` | 视图渲染时进入 Model |
| 22 | `@ControllerAdvice` | `GlobalExceptionHandler`、`GlobalModelAdvice` | `/mvc/ex/*` |
| 23 | 自定义 `HandlerExceptionResolver` | `MyHandlerExceptionResolver`（`extendHandlerExceptionResolvers`） | `/mvc/ex/special` → 409 |
| 24 | `ProblemDetail`（RFC 7807） | `GlobalExceptionHandler#handleOthers` | `/mvc/ex/error` → 500 |
| 25 | 拦截器 `HandlerInterceptor` | `LogInterceptor`（preHandle/postHandle/afterCompletion） | 控制台日志 |
| 26 | 异步拦截器 | `TimingInterceptor#afterConcurrentHandlingStarted` | 访问 `/mvc/async/*` 看日志 |
| 27 | 登录拦截 + exclude | `LoginInterceptor` + `addInterceptors` | `/mvc/secure/profile`（未登录 401） |
| 28 | Filter（Servlet 层） | `RequestIdFilter`、`RequestLogFilter` + `FilterRegistrationBean` | 响应头 `X-Request-Id` |
| 29 | `ContentCachingRequestWrapper` | `RequestLogFilter` | 日志里能看到请求体 |
| 30 | Listener | `MySessionListener`（`@WebListener`）、`MyContextListener`（Java 配置） | `/mvc/monitor/sessions` |
| 31 | 原生 Servlet | `MyServlet` + `ServletRegistrationBean` | `/my-servlet?name=Tom` |
| 32 | 静态资源与缓存 | `WebMvcConfig#addResourceHandlers` | `/static/**`（1 小时缓存）、`/uploads/**`（磁盘目录） |
| 33 | `addViewControllers` | `WebMvcConfig` | `/` → 重定向 index，`/home` → `static:hello` |
| 34 | 自定义参数解析器 | `@CurrentUser` + `CurrentUserArgumentResolver` | `/mvc/advanced/me` |
| 35 | 自定义返回值处理器 | `@WrapResult` + `WrapResultReturnValueHandler` | `/mvc/advanced/list`（自动包 Result） |
| 36 | 自定义 `HttpMessageConverter` | `UserTextHttpMessageConverter`（text/x-user） | `Accept: text/x-user` |
| 37 | 内容协商 | `AdvancedController#negotiate` | 改 `Accept` 看不同结果 |
| 38 | 异步：Callable | `AsyncController#callable` | 约 1.5s 返回 |
| 39 | 异步：WebAsyncTask + 超时回调 | `AsyncController#task` | `/mvc/async/task` |
| 40 | 异步：DeferredResult | `DeferredQueue`（`@Scheduled` 1s 消费） | `/mvc/async/deferred` |
| 41 | 异步：ResponseBodyEmitter / SSE | `AsyncController#emitter`、`/sse` | `curl -N` 或用页面 |
| 42 | 异步：StreamingResponseBody | `AsyncController#stream`、文件流式下载 | `/mvc/async/stream` |
| 43 | `@Async` + CompletableFuture | `AsyncService`、`AsyncConfig` | `/mvc/async/future?name=Spring` |
| 44 | 异步线程池配置 | `configureAsyncSupport` | `mvc-async-*` 线程名 |
| 45 | 文件上传（MultipartFile / 多文件 / @RequestPart JSON / Part） | `FileController` | `pages/file.html` |
| 46 | 文件下载（Resource / byte[] / 流式） | `FileController` | `/mvc/file/download?name=xxx` |
| 47 | 上传大小限制 | `MultipartConfigElement` Bean（5MB / 20MB） | 传大文件返回 413 |
| 48 | 国际化三件套 | `I18nConfig`、`messages_*.properties`、`LocaleChangeInterceptor` | `/mvc/i18n/msg?lang=en_US` |
| 49 | CORS 全局 + 方法级 | `WebMvcConfig#addCorsMappings`、`@CrossOrigin` | OPTIONS 预检看 `Access-Control-*` |
| 50 | 自定义 `/error` | `MyErrorController implements ErrorController` | 访问不存在的地址 |
| 51 | MockMvc 单元测试 | `SpringMvcApplicationTests` | `mvn test` |

---

## 五、目录结构

```
spring-MVC/
├── pom.xml
├── README.md
├── uploads/                        # 运行时生成：上传目录
└── src/
    ├── main/
    │   ├── java/org/example/mvc/
    │   │   ├── SpringMvcApplication.java          # 启动类（@ServletComponentScan + @EnableScheduling）
    │   │   ├── advice/                            # @ControllerAdvice：全局异常、全局 Model/InitBinder、自定义异常解析器
    │   │   ├── annotation/                        # 自定义注解 @CurrentUser / @WrapResult
    │   │   ├── async/                             # DeferredQueue（定时回调）、AsyncService（@Async）
    │   │   ├── common/                            # Result、BizException、SpecialException
    │   │   ├── config/                            # WebMvcConfig（13 个扩展点）、ServletComponentsConfig、AsyncConfig、I18nConfig
    │   │   ├── controller/                        # 14 个 Controller，按知识域拆分
    │   │   ├── convert/                           # Converter、Formatter、PropertyEditor
    │   │   ├── filter/                            # 两个 Filter
    │   │   ├── http/                              # 自定义 HttpMessageConverter
    │   │   ├── interceptor/                       # 3 个拦截器
    │   │   ├── listener/                          # Session / Context 监听器
    │   │   ├── model/                             # User / Address / Dept / FileMeta
    │   │   ├── resolver/                          # 自定义参数解析器、返回值处理器
    │   │   ├── servlet/                           # 原生 HttpServlet
    │   │   ├── validation/                        # 分组、自定义约束、Spring Validator
    │   │   └── view/                              # 自定义 View / ViewResolver
    │   └── resources/
    │       ├── application.yaml                   # 端口 8083、上传目录、异步超时
    │       ├── messages*.properties               # 国际化资源
    │       └── static/                            # 前端页面（无框架）
    └── test/java/org/example/mvc/                 # MockMvc 单元测试 + 容器启动测试
```

---

## 六、接口清单

### 返回值与视图 `/mvc/hello`
| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/json` | @ResponseBody 返回 Result |
| GET | `/text` | 返回纯文本 |
| GET | `/view` | String 视图名 + Model |
| GET | `/mav` | ModelAndView |
| GET | `/forward` | forward:/index.html |
| GET | `/redirect` | redirect + Flash 属性 |
| GET | `/void` | void + 自己写响应 |
| GET | `/servlet-api` | 原生 Request/Session/WebRequest |
| GET | `/created` | @ResponseStatus(201) |

### 参数绑定 `/mvc/bind`
`/simple`、`/map`、`/path/{id}/{slug}`、`/list`、`/pojo`、`/date`、`/header`、`/cookie`、`/attr`、`/servlet-api`、POST `/model-attr`

### 类型转换 `/mvc/convert`
`/dept?dept=10:RD`、`/money?money=1,234.56`、`/number?amount=1234.567`、`/service`

### 校验 `/mvc/validate`
POST `/body`、GET `/form`、POST `/create`、POST `/update`、GET `/spring-validator`

### REST `/mvc/api/users`
| 方法 | 路径 | 状态码 |
| --- | --- | --- |
| GET | `/` | 200 |
| GET | `/{id}` | 200 / 404（带 X-Reason 头） |
| POST | `/` | 201（@ResponseStatus） |
| PUT | `/{id}` | 200 |
| PATCH | `/{id}` | 200 |
| DELETE | `/{id}` | 204 |
| GET | `/view/public`、`/view/admin` | @JsonView |
| GET | `/headers` | 只返回响应头 |
| POST | `/echo` | RequestEntity |
| GET | `/cross` | 方法级 @CrossOrigin |

### 异步 `/mvc/async`
`/callable`、`/task`、`/deferred`、`/emitter`、`/sse`、`/stream`、`/future?name=`

### 文件 `/mvc/file`
POST `/upload`、`/upload-multi`、`/upload-with-meta`、`/upload-part`、`/upload-text`；
GET `/download`、`/download-bytes`、`/download-stream`、`/list`

### 会话 `/mvc/session`
POST `/login`、GET `/me`、GET `/logout`、GET `/cookie/set|read|delete`、GET `/attr`；
`/mvc/secure/profile`（需登录）、`/mvc/secure/public`（放行）；`/mvc/wizard/step|finish`

### 其它
`/mvc/i18n/msg|msg-args|locale|change`、`/mvc/ex/biz|status|type|missing|special|error`、
`/mvc/advanced/me|list|text-user|negotiate`、`/mvc/path/*`、`/mvc/cors/*`、`/mvc/monitor/*`、
`/my-servlet`、`/home`、`/error`

---

## 七、几个关键实现说明

### 1. `WebMvcConfig` 的 13 个扩展点
`addFormatters` / `addInterceptors` / `addResourceHandlers` / `addCorsMappings` /
`addViewControllers` / `configureViewResolvers` / `addArgumentResolvers` /
`addReturnValueHandlers` / `extendMessageConverters` / `configureAsyncSupport` /
`extendHandlerExceptionResolvers` / 静态资源与上传配置。
（`configureDefaultServletHandling` 在嵌入式容器下会报错，代码里保留注释说明，见第九节。）

### 2. 自定义返回值处理器为什么能生效
内置返回值处理器优先，只有**兜底**（把返回值塞进 Model 的那个）排在自定义之后。
所以 `@WrapResult` 的方法返回值不能是 `String`/`Map`/`ResponseEntity`，
否则会被 `ViewNameMethodReturnValueHandler`、`MapMethodProcessor` 等先吃掉。

### 3. 异常处理的三层
```
自定义 MyHandlerExceptionResolver（HIGHEST_PRECEDENCE，只认 SpecialException）
        ↓ 返回 null
ExceptionHandlerExceptionResolver（@ControllerAdvice 的 @ExceptionHandler）
        ↓
ResponseStatusExceptionResolver → DefaultHandlerExceptionResolver
        ↓
容器转发 /error → MyErrorController
```
注册自定义解析器要用 `extendHandlerExceptionResolvers`（追加），
用 `configureHandlerExceptionResolvers`（替换）会把上面这一整套默认解析器全干掉。

### 4. 没有模板引擎时怎么演示 View
`StaticViewResolver` 把 `static:xxx` 解析成 `/pages/xxx.html`，
`StaticView` 把 Model 放进 request 属性后 `forward`。
静态页无法回显属性（这就是为什么 hello.html 里说明了「想回显请接 Thymeleaf」），
但**视图解析、转发、Model 传递这条链路是真实跑通的**（可看日志 `postHandle -> model=[...]`）。

---

## 八、实测结果（本机 curl 验证）

| 场景 | 结果 |
| --- | --- |
| `/`、`/index.html`、`/home` | 302 / 200 / 200 |
| POJO 绑定（嵌套+集合+自定义类型+日期） | `dept={id:10,name:RD}`、`tags=[a,b]`、`vipExpire=2030-12-31` |
| 自定义 Converter / Formatter | `dept=10:RD` → Dept；`1,234.56` → 1234.56 |
| 校验失败 | 400 + `{phone=..., age=..., username=...}` 字段级错误 |
| REST | 列表 200、`/999` 404 + `X-Reason`、POST 201、DELETE 204 |
| @JsonView | Public 4 字段 / Admin 6 字段（含 phone、balance） |
| 异步 | Callable 1.5s 返回；DeferredResult 由定时任务 1s 后回填；Emitter 逐条推送 |
| 异常 | BizException 400 / Special 409 / ResponseStatus 404 / 兜底 ProblemDetail 500 / 类型不匹配 400 |
| 登录 | POST 登录 → session + Flash 消息；未登录 `/mvc/secure/profile` 401，登录后 200 |
| 自定义解析器 | 未登录返回「游客」，登录后返回真实用户 |
| 自定义消息转换器 | `Accept: text/x-user` → `zhangsan:20:张三` |
| 国际化 | zh_CN「欢迎学习 Spring MVC」/ en_US「Welcome to learn Spring MVC」 |
| 路径匹配 | `*`/`**`/正则/`params`/`headers` 全部按预期命中，不匹配返回 404 |
| CORS | OPTIONS 预检返回 `Access-Control-Allow-Origin: http://localhost:5173` |
| 上传下载 | 上传返回保存路径，`/mvc/file/list` 可列出 |
| 运行时 | `X-Request-Id` 响应头存在；`/mvc/monitor/mappings` 共 98 条映射；在线会话 1 |

---

## 九、Spring Boot 4 / Spring 7 踩坑记录（本项目真实遇到）

| 现象 | 原因 | 解决 |
| --- | --- | --- |
| 找不到 `com.fasterxml.jackson.databind.ObjectMapper` | Boot 4 用 Jackson 3，包名改为 `tools.jackson.*`（注解仍在 `com.fasterxml.jackson.*`） | `import tools.jackson.databind.ObjectMapper` |
| 找不到 `org.springframework.boot.web.servlet.ServletComponentScan` | 包迁移 | `org.springframework.boot.web.server.servlet.context.ServletComponentScan` |
| 找不到 `...web.servlet.error.ErrorController` | 包迁移 | `org.springframework.boot.webmvc.error.ErrorController` |
| 找不到 `MultipartConfigFactory` | 拆到独立模块 | `org.springframework.boot.servlet.MultipartConfigFactory` |
| `ContentCachingRequestWrapper(HttpServletRequest)` 编译失败 | Spring 6+ 必须显式给缓存上限 | `new ContentCachingRequestWrapper(request, 8192)` |
| `setTaskExecutor(TaskExecutor)` 类型不匹配 | 需要 `AsyncTaskExecutor` | 字段/Bean 类型改为 `AsyncTaskExecutor` |
| `FormattingConversionService#getConverters()` 不存在 | Spring 7 移除 | 改用 `convert(...) / canConvert(...)` |
| 启动报 “Unable to locate the default servlet” | 嵌入式 Tomcat 下 `configurer.enable()` 找不到 default servlet | 去掉 `configureDefaultServletHandling` |
| `spring-boot:run` 报多个 main 类 | 目录里原有骨架工程 `org.example.springmvc.SpringMvcApplication` | 删除骨架，只保留 `org.example.mvc.SpringMvcApplication` |
| 日期序列化差一天 | Jackson 默认按 UTC 输出 `java.util.Date` | `@JsonFormat(..., timezone = "Asia/Shanghai")` |

---

## 十、变更记录

| 日期 | 类型 | 改动 | 影响面与回滚 |
| --- | --- | --- | --- |
| 2026-10-10 | 新增 | 初始化 spring-MVC 学习项目：14 个 Controller、13 个 WebMvcConfigurer 扩展点、13 个演示页面、9 个单元测试、README | 新增独立模块，不影响其它项目；回滚直接删除 `spring-MVC` 目录 |
| 2026-10-10 | 修复 | 适配 Boot 4 / Spring 7 的包与 API 迁移（见第九节） | 仅本项目；回滚需同时还原 import 与 `WebMvcConfig` |
