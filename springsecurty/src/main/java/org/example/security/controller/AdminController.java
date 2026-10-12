package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.CommentVO;
import org.example.security.dto.PageResult;
import org.example.security.dto.UserVO;
import org.example.security.dto.VideoVO;
import org.example.security.entity.Comment;
import org.example.security.entity.LoginLog;
import org.example.security.entity.Video;
import org.example.security.repository.CommentRepository;
import org.example.security.repository.VideoRepository;
import org.example.security.service.CommentService;
import org.example.security.service.SessionService;
import org.example.security.service.StatsService;
import org.example.security.service.UserService;
import org.example.security.service.VideoService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 管理端接口。
 *
 * <p>双重保护：URL 级（SecurityConfig 里 {@code /app/admin/**} 需要 user:manage 权限）
 * + 方法级（下面每个方法的 @PreAuthorize）。</p>
 */
@RestController
@RequestMapping("/app/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:manage')")
public class AdminController {

    private final UserService userService;
    private final VideoService videoService;
    private final CommentService commentService;
    private final SessionService sessionService;
    private final StatsService statsService;
    private final VideoRepository videoRepository;
    private final CommentRepository commentRepository;

    /* ---------------- 用户管理 ---------------- */

    @GetMapping("/users")
    public Result<PageResult<UserVO>> users(@RequestParam(required = false) String keyword,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return Result.ok(userService.adminList(keyword, page, size));
    }

    /** 启用 / 禁用 */
    @PostMapping("/users/{id}/enabled")
    public Result<String> setEnabled(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        userService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")));
        return Result.ok("已更新状态");
    }

    /** 分配角色 */
    @PostMapping("/users/{id}/roles")
    @SuppressWarnings("unchecked")
    public Result<String> setRoles(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Object rolesObj = body.get("roles");
        Set<String> roles = rolesObj instanceof List<?> list
                ? list.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet())
                : Set.of();
        userService.setRoles(id, roles);
        return Result.ok("角色已更新：" + roles);
    }

    /** 重置密码 */
    @PostMapping("/users/{id}/reset-password")
    public Result<String> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        userService.resetPassword(id, body.get("password"));
        return Result.ok("密码已重置");
    }

    @PostMapping("/users/{id}/unlock")
    public Result<String> unlock(@PathVariable Long id) {
        userService.unlock(id);
        return Result.ok("已解锁");
    }

    @DeleteMapping("/users/{id}")
    public Result<String> removeUser(@PathVariable Long id) {
        userService.remove(id);
        return Result.ok("已删除");
    }

    /* ---------------- 视频管理 ---------------- */

    @GetMapping("/videos")
    public Result<PageResult<VideoVO>> videos(@RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) String status,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "id"));
        var result = videoRepository.searchAdmin(keyword, null, status, pageable);
        List<VideoVO> list = result.getContent().stream().map(v -> {
            VideoVO vo = VideoVO.from(v);
            vo.setCanEdit(true);
            return vo;
        }).toList();
        return Result.ok(PageResult.of(list, result.getTotalElements(), page, size));
    }

    /** 审核：上架 / 下架 / 待审核 */
    @PostMapping("/videos/{id}/status")
    public Result<String> changeStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        videoService.changeStatus(id, body.get("status"));
        return Result.ok("状态已更新");
    }

    @DeleteMapping("/videos/{id}")
    public Result<String> removeVideo(@PathVariable Long id) {
        videoService.delete(id);
        return Result.ok("已删除");
    }

    /* ---------------- 评论管理 ---------------- */

    @GetMapping("/comments")
    public Result<PageResult<CommentVO>> comments(@RequestParam(required = false) String keyword,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        var pageable = PageRequest.of(Math.max(page - 1, 0), Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "id"));
        var result = commentRepository.searchAdmin(keyword, pageable);
        List<CommentVO> list = result.getContent().stream().map(c -> {
            CommentVO vo = CommentVO.from(c);
            vo.setCanDelete(true);
            return vo;
        }).toList();
        return Result.ok(PageResult.of(list, result.getTotalElements(), page, size));
    }

    @DeleteMapping("/comments/{id}")
    public Result<String> removeComment(@PathVariable Long id) {
        commentService.remove(id);
        return Result.ok("已删除");
    }

    /* ---------------- 会话管理：强制下线 ---------------- */

    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.ok(sessionService.onlineUsers());
    }

    @PostMapping("/sessions/kick")
    public Result<String> kick(@RequestParam String username) {
        int count = sessionService.kick(username);
        return Result.ok("已强制下线 " + count + " 个会话");
    }

    /* ---------------- 日志与统计 ---------------- */

    @GetMapping("/logs")
    public Result<List<LoginLog>> logs() {
        return Result.ok(statsService.latestLogs(50));
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(statsService.overview());
    }

    /** 演示 @PostFilter：返回「我能操作的视频」 */
    @GetMapping("/videos/editable")
    public Result<List<VideoVO>> editable() {
        List<Video> raw = videoService.listMineRaw();
        return Result.ok(raw.stream().map(VideoVO::from).toList());
    }
}
