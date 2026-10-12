package org.example.security.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 静态页面路由（页面全部在 static 下，没有模板引擎）。
 *
 * <ul>
 *     <li>用户端页面：static/user/*.html</li>
 *     <li>管理端页面：static/admin/index.html</li>
 * </ul>
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }

    @GetMapping("/user")
    public String userHome() {
        return "redirect:/user/home.html";
    }

    @GetMapping("/admin")
    public String adminHome() {
        return "redirect:/admin/index.html";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/user/login.html";
    }
}
