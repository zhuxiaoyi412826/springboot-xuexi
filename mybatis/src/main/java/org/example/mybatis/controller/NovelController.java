package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.CategoryStatVO;
import org.example.mybatis.dto.NovelDetailVO;
import org.example.mybatis.dto.NovelQuery;
import org.example.mybatis.dto.NovelVO;
import org.example.mybatis.entity.Chapter;
import org.example.mybatis.entity.Novel;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.NovelService;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 小说：前台浏览检索 + 后台 CRUD + 上传 + 统计报表
 */
@RestController
@RequestMapping("/api/novels")
public class NovelController {

    private final NovelService novelService;

    public NovelController(NovelService novelService) {
        this.novelService = novelService;
    }

    // ==================== 前台：浏览 ====================

    /**
     * 多条件检索 + 分页
     *
     * <p>GET 参数会自动绑定到 NovelQuery（包括 List 类型的 tags / categoryIds）。
     */
    @GetMapping
    public Result<PageResult<NovelVO>> search(@ModelAttribute NovelQuery query,
                                              @RequestParam(defaultValue = "1") int pageNum,
                                              @RequestParam(defaultValue = "12") int pageSize,
                                              HttpSession session) {
        User user = SessionUtil.getLoginUser(session);
        // 带上当前用户，后台才知道「我是否收藏过」
        if (user != null) {
            query.setUserId(user.getId());
        }
        return Result.ok(novelService.search(query, pageNum, pageSize));
    }

    /** 详情页（同时浏览量 +1） */
    @GetMapping("/{id}")
    public Result<NovelDetailVO> detail(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.getLoginUser(session);
        Long userId = user == null ? null : user.getId();
        return Result.ok(novelService.detail(id, userId));
    }

    /** 热门 TOP N */
    @GetMapping("/hot")
    public Result<List<NovelVO>> hot(@RequestParam(defaultValue = "8") int limit,
                                     @RequestParam(required = false) Long categoryId) {
        return Result.ok(novelService.hotTop(limit, categoryId));
    }

    // ==================== 后台：增删改 ====================

    @PostMapping
    public Result<Void> save(@RequestBody Novel novel, HttpSession session) {
        SessionUtil.requireAdmin(session);
        novelService.save(novel);
        return Result.ok();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Novel novel, HttpSession session) {
        SessionUtil.requireAdmin(session);
        novelService.update(novel);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        novelService.remove(id);
        return Result.ok();
    }

    @PostMapping("/batch-delete")
    public Result<Void> batchRemove(@RequestBody List<Long> ids, HttpSession session) {
        SessionUtil.requireAdmin(session);
        novelService.batchRemove(ids);
        return Result.ok();
    }

    // ==================== 上传 ====================

    /**
     * 上传 TXT 自动分章
     *
     * <p>表单字段：file（文件）+ title/author/categoryId 等元数据
     */
    @PostMapping("/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                              @RequestParam(required = false) String title,
                                              @RequestParam(required = false) String author,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) String description,
                                              @RequestParam(required = false) String status,
                                              HttpSession session) throws IOException {
        SessionUtil.requireAdmin(session);
        Novel meta = new Novel();
        meta.setTitle(title);
        meta.setAuthor(author);
        meta.setCategoryId(categoryId);
        meta.setDescription(description);
        meta.setStatus(status);
        return Result.ok(novelService.upload(file, meta));
    }

    /** 手动新增章节 */
    @PostMapping("/{id}/chapters")
    public Result<Integer> addChapter(@PathVariable Long id,
                                      @RequestBody Chapter chapter,
                                      HttpSession session) {
        SessionUtil.requireAdmin(session);
        chapter.setNovelId(id);
        return Result.ok(novelService.addChapter(chapter));
    }

    // ==================== 统计报表 ====================

    @GetMapping("/stat/category")
    public Result<List<CategoryStatVO>> categoryStat(HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.categoryStat());
    }

    @GetMapping("/stat/author")
    public Result<Map<String, Object>> authorRank(@RequestParam(defaultValue = "1") int minCount,
                                                  @RequestParam(defaultValue = "10") int limit,
                                                  HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.authorRank(minCount, limit));
    }

    @GetMapping("/stat/overview")
    public Result<Map<String, Object>> overview(HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.overview());
    }

    @GetMapping("/stat/daily-novel")
    public Result<List<Map<String, Object>>> dailyNovel(@RequestParam(defaultValue = "7") int days,
                                                        HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.dailyNovelCount(days));
    }

    @GetMapping("/stat/daily-download")
    public Result<List<Map<String, Object>>> dailyDownload(@RequestParam(defaultValue = "7") int days,
                                                           HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.dailyDownloadCount(days));
    }

    @GetMapping("/stat/user-rank")
    public Result<List<Map<String, Object>>> userRank(@RequestParam(defaultValue = "10") int limit,
                                                      HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(novelService.userActiveRank(limit));
    }
}
