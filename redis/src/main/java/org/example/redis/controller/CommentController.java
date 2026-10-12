package org.example.redis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.Result;
import org.example.redis.service.CommentService;
import org.example.redis.vo.CommentVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 评论（用户端）。 */
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "评论")
public class CommentController {

    private final CommentService commentService;

    @Data
    public static class AddRequest {
        private Long songId;
        private String content;
        /** 幂等 key：同一次提交用同一个值，防重复 */
        private String idempotentKey;
    }

    @GetMapping("/list")
    @Operation(summary = "歌曲评论列表（短 TTL 缓存）")
    public Result<List<CommentVO>> list(@RequestParam Long songId) {
        return Result.ok(commentService.list(songId));
    }

    @PostMapping("/add")
    @Operation(summary = "发评论（令牌桶限流 + 幂等 + Redis 分布式 ID）")
    public Result<CommentVO> add(@RequestBody AddRequest req) {
        return Result.ok(commentService.add(req.getSongId(), req.getContent(), req.getIdempotentKey()));
    }

    @PostMapping("/{id}/like")
    @Operation(summary = "评论点赞")
    public Result<Long> like(@PathVariable Long id) {
        return Result.ok(commentService.like(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除评论（作者本人或管理员）")
    public Result<String> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok("已删除");
    }
}
