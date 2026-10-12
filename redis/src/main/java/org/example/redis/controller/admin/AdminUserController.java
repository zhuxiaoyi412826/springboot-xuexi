package org.example.redis.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.service.UserService;
import org.example.redis.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端：用户管理（禁用/踢下线/封禁都落在 Redis 上）。 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理端-用户")
public class AdminUserController {

    private final UserService userService;

    @GetMapping("/list")
    @Operation(summary = "用户列表")
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(userService.page(page, size, keyword));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "启用/禁用（禁用会立刻踢下线）")
    public Result<String> status(@PathVariable Long id, @RequestParam int status) {
        userService.setStatus(id, status);
        return Result.ok("已更新");
    }

    @PostMapping("/{id}/kickout")
    @Operation(summary = "强制下线（Sa-Token kickout）")
    public Result<String> kickout(@PathVariable Long id) {
        userService.kickout(id);
        return Result.ok("已踢下线");
    }

    @PostMapping("/{id}/ban")
    @Operation(summary = "封禁 N 秒")
    public Result<String> ban(@PathVariable Long id, @RequestParam(defaultValue = "600") long seconds) {
        userService.ban(id, seconds);
        return Result.ok("已封禁 " + seconds + " 秒");
    }

    @PostMapping("/{id}/unban")
    @Operation(summary = "解除封禁")
    public Result<String> unban(@PathVariable Long id) {
        userService.unban(id);
        return Result.ok("已解除");
    }

    @GetMapping("/online")
    @Operation(summary = "在线用户（Redis Set）")
    public Result<Map<String, Object>> online() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", userService.onlineCount());
        data.put("userIds", userService.onlineUsers());
        return Result.ok(data);
    }
}
