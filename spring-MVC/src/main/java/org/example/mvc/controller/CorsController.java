package org.example.mvc.controller;

import org.example.mvc.common.Result;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * CORS：方法级 @CrossOrigin 与全局配置（WebMvcConfig#addCorsMappings）对比。
 */
@RestController
@RequestMapping("/mvc/cors")
public class CorsController {

    /** 走全局 CORS 配置（/mvc/cors/**） */
    @GetMapping("/global")
    public Result global() {
        return Result.ok("由全局 addCorsMappings 放行");
    }

    /** 方法级 @CrossOrigin：只允许指定来源，并暴露自定义响应头 */
    @CrossOrigin(origins = {"http://127.0.0.1:5500", "http://localhost:5173"},
            allowedHeaders = "*",
            exposedHeaders = "X-Custom-Header",
            allowCredentials = "true",
            maxAge = 1800)
    @PostMapping("/annotation")
    public Result annotation() {
        return Result.ok(Map.of("msg", "由方法上的 @CrossOrigin 放行"));
    }
}
