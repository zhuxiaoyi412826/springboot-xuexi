package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.entity.Category;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.service.CategoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理：前台所有人可查，增删改需要管理员
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /** 分类列表（可读我曾经命中二级缓存） */
    @GetMapping
    public Result<List<Category>> list() {
        return Result.ok(categoryService.listAll());
    }

    /** 分类 + 旗下小说（XML <collection> 一对多） */
    @GetMapping("/with-novels")
    public Result<List<Category>> withNovels() {
        return Result.ok(categoryService.listWithNovels());
    }

    /** 只含小说的分类（HAVING 演示） */
    @GetMapping("/non-empty")
    public Result<List<Category>> nonEmpty() {
        return Result.ok(categoryService.listNonEmpty());
    }

    @PostMapping
    public Result<Void> save(@RequestBody Category category, HttpSession session) {
        SessionUtil.requireAdmin(session);
        categoryService.save(category);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Category category, HttpSession session) {
        SessionUtil.requireAdmin(session);
        categoryService.update(category);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        categoryService.remove(id);
        return Result.ok();
    }
}
