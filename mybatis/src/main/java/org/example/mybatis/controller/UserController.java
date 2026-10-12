package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.UserQuery;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.UserService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 用户管理（后台）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 分页 + 多条件查询 */
    @GetMapping
    public Result<PageResult<User>> page(@ModelAttribute UserQuery query,
                                         @RequestParam(defaultValue = "1") int pageNum,
                                         @RequestParam(defaultValue = "10") int pageSize,
                                         HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(userService.page(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(userService.getById(id));
    }

    @PostMapping
    public Result<Void> save(@RequestBody User user, HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.register(user);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody User user, HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.update(user);
        return Result.ok();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestParam Integer status,
                                     HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.updateStatus(id, status);
        return Result.ok();
    }

    @PutMapping("/{id}/password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestParam String password,
                                      HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.resetPassword(id, password);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.remove(id);
        return Result.ok();
    }

    /** 批量删除（XML foreach） */
    @PostMapping("/batch-delete")
    public Result<Void> batchRemove(@RequestBody List<Long> ids, HttpSession session) {
        SessionUtil.requireAdmin(session);
        userService.batchRemove(ids);
        return Result.ok();
    }
}
