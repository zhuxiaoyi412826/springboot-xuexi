package org.example.redis.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.service.PlaylistService;
import org.example.redis.vo.PlaylistVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 管理端：歌单审核（待审核歌单放在 Redis List 队列里）。 */
@RestController
@RequestMapping("/api/admin/playlists")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理端-歌单")
public class AdminPlaylistController {

    private final PlaylistService playlistService;

    @GetMapping("/list")
    @Operation(summary = "歌单列表")
    public Result<PageResult<PlaylistVO>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "10") int size) {
        return Result.ok(playlistService.adminPage(page, size));
    }

    @GetMapping("/audit-queue")
    @Operation(summary = "待审核歌单 id 列表（Redis List）")
    public Result<List<Long>> auditQueue() {
        return Result.ok(playlistService.auditQueue());
    }

    @PostMapping("/{id}/audit")
    @Operation(summary = "审核：1 通过 0 拒绝")
    public Result<String> audit(@PathVariable Long id, @RequestParam int status) {
        playlistService.audit(id, status);
        return Result.ok("已审核");
    }
}
