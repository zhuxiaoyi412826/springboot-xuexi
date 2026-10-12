package org.example.mvc.controller;

import org.example.mvc.annotation.CurrentUser;
import org.example.mvc.annotation.WrapResult;
import org.example.mvc.common.Result;
import org.example.mvc.model.User;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 扩展点演示：
 * <ul>
 *     <li>自定义参数解析器：{@code @CurrentUser User user}</li>
 *     <li>自定义返回值处理器：类上的 {@code @WrapResult} 会把返回值自动包装成 Result</li>
 *     <li>自定义 HttpMessageConverter：text/x-user 媒体类型</li>
 * </ul>
 *
 * <p>注意：本类用 @Controller 而不是 @RestController，且返回值故意不是 String/Map/ResponseEntity，
 * 这样才会命中自定义的返回值处理器（内置处理器优先）。</p>
 */
@Controller
@WrapResult
@RequestMapping("/mvc/advanced")
public class AdvancedController {

    /** 1. 自定义参数解析器：未登录返回游客对象 */
    @GetMapping("/me")
    public User me(@CurrentUser User user) {
        return user;
    }

    /** 2. 自定义返回值处理器：返回 List<User>，被自动包装成 {"code":0,"msg":"ok","data":[...]} */
    @GetMapping("/list")
    public List<User> list() {
        List<User> users = new ArrayList<>();
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("zhangsan");
        u1.setAge(20);
        u1.setBalance(new BigDecimal("12.30"));
        users.add(u1);
        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("lisi");
        u2.setAge(30);
        users.add(u2);
        return users;
    }

    /** 3. 自定义消息转换器（写）：Accept: text/x-user */
    @GetMapping(value = "/text-user", produces = "text/x-user")
    @ResponseBody
    public User textUser() {
        User user = new User();
        user.setUsername("zhangsan");
        user.setAge(20);
        user.setNickname("张三");
        return user;
    }

    /** 4. 自定义消息转换器（读）：Content-Type: text/x-user，请求体 "wangwu:25:王五" */
    @PostMapping(value = "/text-user", consumes = "text/x-user")
    @ResponseBody
    public Result readTextUser(@RequestBody User user) {
        return Result.ok(user);
    }

    /** 5. 内容协商：同一个地址根据 Accept 返回不同形式（此处由 @ResponseBody + 转换器自动完成） */
    @GetMapping(value = "/negotiate", produces = {MediaType.APPLICATION_JSON_VALUE, "text/x-user"})
    @ResponseBody
    public User negotiate() {
        User user = new User();
        user.setUsername("negotiate");
        user.setAge(18);
        user.setNickname("内容协商");
        return user;
    }
}
