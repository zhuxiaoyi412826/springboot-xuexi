package org.example.redis.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.Result;
import org.example.redis.listener.NoticeListener;
import org.example.redis.service.CacheManageService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 管理端：缓存与 Redis 运维面板。 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理端-缓存/Redis")
public class AdminCacheController {

    private final CacheManageService cacheManageService;
    private final NoticeListener noticeListener;

    @GetMapping("/overview")
    @Operation(summary = "总览：命中率 + L1 指标 + Redis INFO")
    public Result<Map<String, Object>> overview() {
        return Result.ok(cacheManageService.overview());
    }

    @PostMapping("/clear")
    @Operation(summary = "按缓存名清理（如 song、songList、comment）")
    public Result<Long> clear(@RequestParam String name) {
        return Result.ok(cacheManageService.clear(name));
    }

    @PostMapping("/warm")
    @Operation(summary = "手动预热")
    public Result<String> warm() {
        return Result.ok(cacheManageService.warm());
    }

    @PostMapping("/level")
    @Operation(summary = "动态开关缓存层级：l1 / l2 / l3 / bloom")
    public Result<Map<String, Object>> level(@RequestParam String level, @RequestParam boolean enabled) {
        return Result.ok(cacheManageService.switchLevel(level, enabled));
    }

    @GetMapping("/keys")
    @Operation(summary = "浏览 key（SCAN）")
    public Result<List<Map<String, Object>>> keys(@RequestParam(defaultValue = "music:*") String pattern,
                                                  @RequestParam(defaultValue = "100") int count) {
        return Result.ok(cacheManageService.keys(pattern, count));
    }

    @DeleteMapping("/key")
    @Operation(summary = "删除指定 key（只允许 music: 前缀）")
    public Result<Boolean> deleteKey(@RequestParam String key) {
        return Result.ok(cacheManageService.deleteKey(key));
    }

    @PostMapping("/reset-stats")
    @Operation(summary = "重置命中率统计")
    public Result<String> reset() {
        cacheManageService.resetStats();
        return Result.ok("已重置");
    }

    @GetMapping("/notices")
    @Operation(summary = "最近收到的 Pub/Sub 通知")
    public Result<List<String>> notices() {
        return Result.ok(noticeListener.latest());
    }
}
