package org.example.redis.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.service.CommentService;
import org.example.redis.vo.CommentVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端：评论管理。 */
@RestController
@RequestMapping("/api/admin/comments")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理端-评论")
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping("/list")
    @Operation(summary = "评论列表")
    public Result<PageResult<CommentVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestParam(required = false) String keyword) {
        return Result.ok(commentService.adminPage(page, size, keyword));
    }

    @PostMapping("/{id}/shield")
    @Operation(summary = "屏蔽/放开评论")
    public Result<String> shield(@PathVariable Long id, @RequestParam int status) {
        commentService.shield(id, status);
        return Result.ok("已更新");
    }

    @PostMapping("/{id}/delete")
    @Operation(summary = "删除评论")
    public Result<String> delete(@PathVariable Long id) {
        commentService.delete(id);
        return Result.ok("已删除");
    }
}
