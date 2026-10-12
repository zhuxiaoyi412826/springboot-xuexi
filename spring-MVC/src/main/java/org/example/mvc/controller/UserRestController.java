package org.example.mvc.controller;

import com.fasterxml.jackson.annotation.JsonView;
import jakarta.validation.Valid;
import org.example.mvc.common.BizException;
import org.example.mvc.model.User;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * REST 风格接口：@RestController（= @Controller + @ResponseBody）、
 * ResponseEntity、HttpEntity/RequestEntity、@ResponseStatus、@JsonView、consumes/produces。
 */
@RestController
@RequestMapping(value = "/mvc/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserRestController {

    private final Map<Long, User> store = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong(2);

    public UserRestController() {
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("zhangsan");
        u1.setNickname("张三");
        u1.setAge(20);
        u1.setPhone("13800138000");
        u1.setBalance(new BigDecimal("88.80"));
        u1.setBirthday(LocalDate.of(2000, 1, 1));
        u1.setCreateTime(LocalDateTime.now());
        store.put(1L, u1);

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("lisi");
        u2.setNickname("李四");
        u2.setAge(30);
        u2.setPhone("13900139000");
        u2.setBalance(new BigDecimal("168.00"));
        u2.setBirthday(LocalDate.of(1990, 5, 20));
        u2.setCreateTime(LocalDateTime.now());
        store.put(2L, u2);
    }

    /** 列表 */
    @GetMapping
    public List<User> list() {
        return new ArrayList<>(store.values());
    }

    /** 单个资源，配合 ResponseEntity 控制状态码与响应头 */
    @GetMapping("/{id}")
    public ResponseEntity<User> get(@PathVariable Long id) {
        User user = store.get(id);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Reason", "user not found")
                    .body(null);
        }
        return ResponseEntity.ok()
                .header("X-Resource-Id", String.valueOf(id))
                .body(user);
    }

    /** 新增：@ResponseStatus 指定 201；consumes 限制只接收 JSON */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public User create(@Valid @RequestBody User user) {
        long id = seq.incrementAndGet();
        user.setId(id);
        user.setCreateTime(LocalDateTime.now());
        store.put(id, user);
        return user;
    }

    /** 全量更新 */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody User user) {
        if (!store.containsKey(id)) {
            throw new BizException(2001, "用户不存在，无法更新");
        }
        user.setId(id);
        store.put(id, user);
        return user;
    }

    /** 局部更新：用 Map 接收任意字段 */
    @PatchMapping("/{id}")
    public User patch(@PathVariable Long id, @RequestBody Map<String, Object> patch) {
        User user = store.get(id);
        if (user == null) {
            throw new BizException(2001, "用户不存在");
        }
        if (patch.get("nickname") != null) {
            user.setNickname(String.valueOf(patch.get("nickname")));
        }
        if (patch.get("age") != null) {
            user.setAge(Integer.valueOf(String.valueOf(patch.get("age"))));
        }
        return user;
    }

    /** 删除：204 No Content */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        store.remove(id);
    }

    /** @JsonView：只输出 Public 视图字段 */
    @GetMapping("/view/public")
    @JsonView(User.Views.Public.class)
    public List<User> publicView() {
        return new ArrayList<>(store.values());
    }

    /** @JsonView：输出 Admin 视图字段（包含 phone / balance 等敏感字段） */
    @GetMapping("/view/admin")
    @JsonView(User.Views.Admin.class)
    public List<User> adminView() {
        return new ArrayList<>(store.values());
    }

    /** HttpHeaders 作为返回值：只返回头，无响应体 */
    @GetMapping("/headers")
    public ResponseEntity<Void> headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Total-Count", String.valueOf(store.size()));
        headers.add("X-App", "spring-mvc-lab");
        return new ResponseEntity<>(headers, HttpStatus.OK);
    }

    /** RequestEntity：一次性拿到请求头 + 请求体 */
    @PostMapping("/echo")
    public Map<String, Object> echo(RequestEntity<User> requestEntity) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("url", String.valueOf(requestEntity.getUrl()));
        data.put("method", requestEntity.getMethod());
        data.put("contentType", requestEntity.getHeaders().getContentType());
        data.put("body", requestEntity.getBody());
        return data;
    }

    /** 方法级 CORS（全局 CORS 见 WebMvcConfig#addCorsMappings） */
    @CrossOrigin(origins = "http://localhost:5173", maxAge = 1800)
    @GetMapping("/cross")
    public Map<String, Object> cross() {
        return Map.of("msg", "该接口允许 http://localhost:5173 跨域访问");
    }
}
