package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.security.SecurityUtil;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证方式演示接口：
 * <ul>
 *     <li>{@code GET /digest/me}：HTTP Digest 摘要认证（curl --digest -u admin:admin123）</li>
 *     <li>{@code GET /app/basic/me}：HTTP Basic 认证（curl -u admin:admin123）</li>
 * </ul>
 */
@RestController
@RequiredArgsConstructor
public class DemoController {

    @GetMapping("/digest/me")
    public Result<Map<String, Object>> digestMe(Authentication authentication) {
        return Result.ok(info(authentication, "digest"));
    }

    @GetMapping("/app/basic/me")
    public Result<Map<String, Object>> basicMe(Authentication authentication) {
        return Result.ok(info(authentication, "basic"));
    }

    private Map<String, Object> info(Authentication authentication, String type) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("authType", type);
        data.put("username", authentication == null ? null : authentication.getName());
        data.put("principal", authentication == null ? null : String.valueOf(authentication.getPrincipal()));
        data.put("authorities", authentication == null ? List.of()
                : authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        data.put("userId", SecurityUtil.currentUserId());
        return data;
    }
}
