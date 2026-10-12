package org.example.redis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.example.redis.common.Result;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 健康检查（启动自检用）。 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "健康检查")
public class PingController {

    private final StringRedisTemplate redis;

    public PingController(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @GetMapping("/ping")
    @Operation(summary = "健康检查：验证 Redis 连通性")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        try {
            redis.opsForValue().set("music:ping", "pong");
            data.put("redis", redis.opsForValue().get("music:ping"));
        } catch (Exception e) {
            data.put("redis", "ERROR: " + e.getMessage());
        }
        data.put("app", "music-redis");
        return Result.ok(data);
    }
}
