package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.FavoriteVO;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.FavoriteService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 收藏
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    /** 收藏 / 取消收藏（切换） */
    @PostMapping("/toggle/{novelId}")
    public Result<Map<String, Object>> toggle(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        boolean favorited = favoriteService.toggle(user.getId(), novelId);
        Map<String, Object> data = new HashMap<>();
        data.put("favorited", favorited);
        data.put("count", favoriteService.count(user.getId()));
        return Result.ok(data);
    }

    /** 是否已收藏 */
    @GetMapping("/check/{novelId}")
    public Result<Boolean> check(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.getLoginUser(session);
        return Result.ok(favoriteService.isFavorited(user == null ? null : user.getId(), novelId));
    }

    /** 我的收藏（分页 + 多表关联） */
    @GetMapping
    public Result<PageResult<FavoriteVO>> my(@RequestParam(defaultValue = "1") int pageNum,
                                             @RequestParam(defaultValue = "12") int pageSize,
                                             HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(favoriteService.myFavorites(user.getId(), pageNum, pageSize));
    }

    /** 取消收藏 */
    @DeleteMapping("/{novelId}")
    public Result<Void> cancel(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        favoriteService.cancel(user.getId(), novelId);
        return Result.ok();
    }

    @GetMapping("/count")
    public Result<Long> count(HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(favoriteService.count(user.getId()));
    }
}
