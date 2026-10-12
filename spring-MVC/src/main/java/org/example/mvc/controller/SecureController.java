package org.example.mvc.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mvc.common.Result;
import org.example.mvc.resolver.CurrentUserArgumentResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 受登录拦截器保护的资源：/mvc/secure/**（/mvc/secure/public 被 exclude）。
 */
@RestController
@RequestMapping("/mvc/secure")
public class SecureController {

    @GetMapping("/public")
    public Result publicApi() {
        return Result.ok("无需登录也能访问（excludePathPatterns 排除）");
    }

    @GetMapping("/profile")
    public Result profile(HttpSession session) {
        return Result.ok(session.getAttribute(CurrentUserArgumentResolver.SESSION_KEY));
    }
}
