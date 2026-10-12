package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.example.security.dto.CommentVO;
import org.example.security.service.CommentService;
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

/** 评论（删除走对象级权限：作者本人或管理员） */
@RestController
@RequestMapping("/app/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/list")
    public Result<List<CommentVO>> list(@RequestParam Long videoId) {
        return Result.ok(commentService.list(videoId));
    }

    @GetMapping("/mine")
    public Result<List<CommentVO>> mine() {
        return Result.ok(commentService.mine());
    }

    @PostMapping("/add")
    public Result<CommentVO> add(@RequestBody Map<String, String> body) {
        Long videoId = Long.valueOf(body.get("videoId"));
        return Result.ok(commentService.add(videoId, body.get("content")));
    }

    /** 删除：@PreAuthorize("hasPermission(#id,'COMMENT','delete')") */
    @DeleteMapping("/{id}")
    public Result<String> remove(@PathVariable Long id) {
        commentService.remove(id);
        return Result.ok("已删除");
    }
}
