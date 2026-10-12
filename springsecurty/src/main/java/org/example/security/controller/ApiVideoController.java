package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.PageResult;
import org.example.security.dto.VideoVO;
import org.example.security.entity.Video;
import org.example.security.security.SecurityUtil;
import org.example.security.service.VideoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 资源服务器接口（{@code /api/**}）：无状态，只认 {@code Authorization: Bearer <JWT>} 或 {@code X-API-Key}。
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiVideoController {

    private final VideoService videoService;

    /** 探活：可用 JWT，也可用 API Key（自定义过滤器） */
    @GetMapping("/ping")
    public Result<Map<String, Object>> ping(Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("message", "pong");
        data.put("principal", authentication == null ? "匿名" : authentication.getName());
        data.put("authorities", authentication == null ? List.of()
                : authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        return Result.ok(data);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(Authentication authentication) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("username", authentication == null ? null : authentication.getName());
        data.put("userId", SecurityUtil.currentUserId());
        data.put("authorities", authentication == null ? List.of()
                : authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
        return Result.ok(data);
    }

    @GetMapping("/videos")
    public Result<PageResult<VideoVO>> videos(@RequestParam(required = false) String keyword,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        return Result.ok(videoService.list(keyword, null, "new", page, size));
    }

    /** 需要 JWT 里的 video:write 权限 */
    @PostMapping("/videos")
    @PreAuthorize("hasAuthority('video:write')")
    public Result<VideoVO> create(@RequestBody Video video) {
        return Result.ok(VideoVO.from(videoService.create(video)));
    }
}
