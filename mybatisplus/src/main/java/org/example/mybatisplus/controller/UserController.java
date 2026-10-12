package org.example.mybatisplus.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpSession;
import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.common.Result;
import org.example.mybatisplus.common.SessionUtil;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.service.UserService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理（仅管理员可用）
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 用户列表（分页 + 关键字） */
    @GetMapping
    public Result<IPage<User>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpSession session) {
        SessionUtil.requireAdmin(session);
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw));
        }
        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role.trim());
        }
        wrapper.orderByDesc(User::getId);
        IPage<User> page = userService.page(new Page<>(current, size), wrapper);
        page.getRecords().forEach(u -> u.setPassword(null));
        return Result.ok(page);
    }

    /** 新增用户（可指定角色） */
    @PostMapping
    public Result<User> save(@RequestBody User user, HttpSession session) {
        SessionUtil.requireAdmin(session);
        user.setId(null);
        userService.saveUser(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    /** 修改用户 */
    @PutMapping("/{id}")
    public Result<User> update(@PathVariable Long id, @RequestBody User user, HttpSession session) {
        SessionUtil.requireAdmin(session);
        user.setId(id);
        userService.saveUser(user);
        user.setPassword(null);
        return Result.ok(user);
    }

    /** 删除用户（不能删自己） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id, HttpSession session) {
        User loginUser = SessionUtil.requireAdmin(session);
        if (loginUser.getId().equals(id)) {
            throw new BizException("不能删除当前登录的管理员");
        }
        userService.removeById(id);
        return Result.ok();
    }
}
