package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.CommentVO;
import org.example.security.dto.UserVO;
import org.example.security.dto.VideoVO;
import org.example.security.entity.Favorite;
import org.example.security.entity.LoginLog;
import org.example.security.repository.FavoriteRepository;
import org.example.security.repository.LoginLogRepository;
import org.example.security.repository.VideoRepository;
import org.example.security.security.SecurityUtil;
import org.example.security.service.CommentService;
import org.example.security.service.SessionService;
import org.example.security.service.UserService;
import org.example.security.service.VideoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 个人中心：资料、改密、我的收藏/评论/视频/会话 */
@RestController
@RequestMapping("/app/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;
    private final VideoService videoService;
    private final CommentService commentService;
    private final FavoriteRepository favoriteRepository;
    private final VideoRepository videoRepository;
    private final LoginLogRepository loginLogRepository;
    private final SessionService sessionService;

    @GetMapping("/profile")
    public Result<UserVO> profile() {
        return Result.ok(userService.profile());
    }

    @PutMapping("/profile")
    public Result<String> updateProfile(@RequestBody Map<String, String> body) {
        userService.updateProfile(body.get("nickname"), body.get("email"), body.get("phone"), body.get("avatar"));
        return Result.ok("资料已更新");
    }

    @PostMapping("/password")
    public Result<String> changePassword(@RequestBody Map<String, String> body) {
        userService.changePassword(body.get("oldPassword"), body.get("newPassword"));
        return Result.ok("密码已修改，请重新登录");
    }

    @GetMapping("/favorites")
    public Result<List<VideoVO>> favorites() {
        Long uid = SecurityUtil.currentUserId();
        List<VideoVO> list = favoriteRepository.findByUserIdOrderByIdDesc(uid).stream()
                .map(Favorite::getVideoId)
                .map(videoRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(v -> VideoVO.from(v.get()))
                .toList();
        return Result.ok(list);
    }

    @GetMapping("/comments")
    public Result<List<CommentVO>> comments() {
        return Result.ok(commentService.mine());
    }

    @GetMapping("/videos")
    public Result<List<VideoVO>> videos() {
        return Result.ok(videoService.mine());
    }

    /** 我的会话（可下线其它设备） */
    @GetMapping("/sessions")
    public Result<List<Map<String, Object>>> sessions() {
        return Result.ok(sessionService.mySessions());
    }

    @DeleteMapping("/sessions/{sessionId}")
    public Result<String> expireSession(@PathVariable String sessionId) {
        boolean ok = sessionService.expireMySession(sessionId);
        return ok ? Result.ok("该会话已下线") : Result.fail(404, "会话不存在或已失效");
    }

    /** 我的登录日志 */
    @GetMapping("/login-logs")
    public Result<List<LoginLog>> loginLogs() {
        String username = SecurityUtil.currentUsername();
        return Result.ok(loginLogRepository.findTop20ByUsernameOrderByIdDesc(username));
    }
}
