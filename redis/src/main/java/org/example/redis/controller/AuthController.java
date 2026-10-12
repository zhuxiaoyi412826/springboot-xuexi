package org.example.redis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.Result;
import org.example.redis.service.UserService;
import org.example.redis.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录/注册（用户端与管理端共用同一个登录页）。
 *
 * <p>登录成功后返回 satoken，前端把它放到请求头 {@code satoken} 或 {@code Authorization} 里。
 * 跳转哪个页面由后端返回的 role 决定：admin -> /admin/index.html，user -> /user/index.html。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "登录注册")
public class AuthController {

    private final UserService userService;

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Data
    public static class RegisterRequest {
        private String username;
        private String password;
        private String nickname;
    }

    @PostMapping("/login")
    @Operation(summary = "登录（返回 token 和角色）")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest req) {
        UserVO user = userService.login(req.getUsername(), req.getPassword());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", StpUtil.getTokenValue());
        data.put("tokenName", StpUtil.getTokenName());
        data.put("user", user);
        data.put("home", "admin".equals(user.getRole()) ? "/admin/index.html" : "/user/index.html");
        return Result.ok(data);
    }

    @PostMapping("/register")
    @Operation(summary = "注册（默认普通用户）")
    public Result<UserVO> register(@RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req.getUsername(), req.getPassword(), req.getNickname()));
    }

    @PostMapping("/logout")
    @SaCheckLogin
    @Operation(summary = "退出登录")
    public Result<String> logout() {
        userService.logout();
        return Result.ok("已退出");
    }

    @GetMapping("/info")
    @SaCheckLogin
    @Operation(summary = "当前登录用户信息")
    public Result<Map<String, Object>> info() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", StpUtil.getLoginId());
        data.put("roles", StpUtil.getRoleList());
        data.put("permissions", StpUtil.getPermissionList());
        data.put("user", userService.currentUser());
        data.put("tokenTimeout", StpUtil.getTokenTimeout());
        return Result.ok(data);
    }
}
