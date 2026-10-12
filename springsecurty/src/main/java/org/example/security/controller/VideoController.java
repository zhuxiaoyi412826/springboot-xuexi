package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.PageResult;
import org.example.security.dto.VideoVO;
import org.example.security.entity.Video;
import org.example.security.service.VideoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 视频浏览（用户端）。
 *
 * <p>URL 级权限在 SecurityConfig 中配置：
 * 列表/详情匿名可访问，点赞收藏需要 favorite:write，下载需要 video:download（VIP）。</p>
 */
@RestController
@RequestMapping("/app/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** 列表（匿名可访问；登录/管理员看到的数据范围不同 —— 数据权限） */
    @GetMapping("/list")
    public Result<PageResult<VideoVO>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String category,
                                            @RequestParam(defaultValue = "new") String sort,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "12") int size) {
        return Result.ok(videoService.list(keyword, category, sort, page, size));
    }

    @GetMapping("/hot")
    public Result<List<VideoVO>> hot() {
        return Result.ok(videoService.hot());
    }

    /** 详情（非公开视频需要作者本人或管理员 —— 对象级权限在 Service 内判断） */
    @GetMapping("/{id}")
    public Result<VideoVO> detail(@PathVariable Long id) {
        return Result.ok(videoService.detail(id));
    }

    @PostMapping("/{id}/like")
    public Result<String> like(@PathVariable Long id) {
        videoService.like(id);
        return Result.ok("已点赞");
    }

    /** 收藏 / 取消收藏 */
    @PostMapping("/{id}/favorite")
    public Result<Map<String, Object>> favorite(@PathVariable Long id) {
        boolean favorited = videoService.toggleFavorite(id);
        return Result.ok(Map.of("favorited", favorited));
    }

    /** VIP 才能下载（URL 级权限：hasAuthority('video:download')） */
    @PostMapping("/{id}/download")
    @PreAuthorize("hasAuthority('video:download')")
    public Result<Map<String, Object>> download(@PathVariable Long id) {
        VideoVO vo = videoService.detail(id);
        return Result.ok(Map.of("videoId", id, "url", vo.getVideoUrl(), "tip", "VIP 专属下载链接"));
    }

    /** 我上传的视频 */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Result<List<VideoVO>> mine() {
        return Result.ok(videoService.mine());
    }

    /** 新增视频（上传） */
    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Result<VideoVO> add(@org.springframework.web.bind.annotation.RequestBody Video video) {
        return Result.ok(VideoVO.from(videoService.create(video)));
    }
}
