package org.example.mybatisplus.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatisplus.common.Result;
import org.example.mybatisplus.common.SessionUtil;
import org.example.mybatisplus.dto.LoginRequest;
import org.example.mybatisplus.dto.RegisterRequest;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录、注册、退出、当前登录人
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 登录：管理员账号返回 role=ADMIN，前端跳转到后台页面
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request, HttpSession session) {
        User user = userService.login(request.getUsername(), request.getPassword());
        user.setPassword(null);
        SessionUtil.setLoginUser(session, user);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        // 前端根据首页决定跳转
        data.put("home", user.isAdmin() ? "/admin.html" : "/index.html");
        return Result.ok(data);
    }

    /**
     * 注册：只能注册普通用户
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody RegisterRequest request, HttpSession session) {
        User user = userService.register(request.getUsername(), request.getPassword(),
                request.getNickname(), request.getPhone(), request.getAddress());
        user.setPassword(null);
        SessionUtil.setLoginUser(session, user);
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("home", "/index.html");
        return Result.ok(data);
    }

    /**
     * 当前登录用户（未登录返回 null）
     */
    @GetMapping("/info")
    public Result<User> info(HttpSession session) {
        User user = SessionUtil.getLoginUser(session);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.ok(user);
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpSession session) {
        session.invalidate();
        return Result.ok();
    }
}
