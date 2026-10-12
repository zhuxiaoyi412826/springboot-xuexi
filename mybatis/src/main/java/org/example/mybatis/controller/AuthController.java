package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录 / 注册 / 退出
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /** 登录：管理员返回后台页，普通用户返回书城页 */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestParam String username,
                                             @RequestParam String password,
                                             HttpSession session) {
        User user = userService.login(username, password);
        SessionUtil.login(session, user);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("home", user.isAdmin() ? "admin.html" : "index.html");
        return Result.ok(data);
    }

    /** 注册：只能注册普通用户 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestParam String username,
                                                @RequestParam String password,
                                                @RequestParam(required = false) String nickname,
                                                HttpSession session) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setNickname(nickname);
        User saved = userService.register(user);
        SessionUtil.login(session, saved);
        Map<String, Object> data = new HashMap<>();
        data.put("user", saved);
        data.put("home", "index.html");
        return Result.ok(data);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        SessionUtil.logout(session);
        return Result.ok();
    }

    /** 当前登录用户，未登录返回 null */
    @GetMapping("/info")
    public Result<User> info(HttpSession session) {
        return Result.ok(SessionUtil.getLoginUser(session));
    }
}
