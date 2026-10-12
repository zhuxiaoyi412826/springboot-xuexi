package org.example.mybatis.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转
 */
@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "redirect:/login.html";
    }
}
