package org.example.redis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.service.PlaylistService;
import org.example.redis.vo.PlaylistVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 歌单（用户端）。 */
@RestController
@RequestMapping("/api/playlists")
@RequiredArgsConstructor
@Tag(name = "歌单")
public class PlaylistController {

    private final PlaylistService playlistService;

    @Data
    public static class CreateRequest {
        private String name;
        private String description;
        private String coverUrl;
    }

    @GetMapping("/list")
    @Operation(summary = "公开歌单列表")
    public Result<PageResult<PlaylistVO>> list(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "12") int size) {
        return Result.ok(playlistService.page(page, size));
    }

    @GetMapping("/{id}")
    @Operation(summary = "歌单详情（歌曲列表存 Redis List）")
    public Result<PlaylistVO> detail(@PathVariable Long id) {
        return Result.ok(playlistService.detail(id));
    }

    @PostMapping("/create")
    @Operation(summary = "创建歌单（进入待审核队列）")
    public Result<PlaylistVO> create(@RequestBody CreateRequest req) {
        return Result.ok(playlistService.create(req.getName(), req.getDescription(), req.getCoverUrl()));
    }

    @PostMapping("/{id}/songs/{songId}")
    @Operation(summary = "往歌单里加歌曲")
    public Result<String> addSong(@PathVariable Long id, @PathVariable Long songId) {
        playlistService.addSong(id, songId);
        return Result.ok("已添加");
    }
}
