package org.example.mybatisplus.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面入口：访问根路径直接进入登录页
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }
}
