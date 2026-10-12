package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.ChapterQuery;
import org.example.mybatis.entity.Chapter;
import org.example.mybatis.service.ChapterService;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 章节：前台阅读 + 后台管理
 */
@RestController
@RequestMapping("/api/chapters")
public class ChapterController {

    private final ChapterService chapterService;

    public ChapterController(ChapterService chapterService) {
        this.chapterService = chapterService;
    }

    /** 目录：不分页，阅读器左侧用 */
    @GetMapping("/catalog/{novelId}")
    public Result<List<Chapter>> catalog(@PathVariable Long novelId) {
        return Result.ok(chapterService.catalog(novelId));
    }

    /** 目录：分页（后台管理，演示分页插件） */
    @GetMapping("/catalog-page/{novelId}")
    public Result<PageResult<Chapter>> catalogPage(@PathVariable Long novelId,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(chapterService.catalogPage(novelId, pageNum, pageSize));
    }

    /** 动态条件查询章节 */
    @GetMapping("/search")
    public Result<List<Chapter>> search(@ModelAttribute ChapterQuery query, HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(chapterService.search(query));
    }

    /** 阅读某一章内容 */
    @GetMapping("/{id}")
    public Result<Map<String, Object>> read(@PathVariable Long id) {
        Chapter chapter = chapterService.read(id);
        Map<String, Object> data = new HashMap<>();
        data.put("chapter", chapter);
        data.put("prev", chapterService.prev(chapter.getNovelId(), chapter.getChapterNo()));
        Chapter next = chapterService.next(chapter.getNovelId(), chapter.getChapterNo());
        data.put("next", next);
        // 下一章只需 id 与标题，避免把正文也传回去
        if (next != null) {
            data.put("nextId", next.getId());
            data.put("nextTitle", next.getTitle());
            next.setContent(null);
        }
        Chapter prev = (Chapter) data.get("prev");
        if (prev != null) {
            data.put("prevId", prev.getId());
            prev.setContent(null);
        }
        return Result.ok(data);
    }

    /** 按序号阅读 */
    @GetMapping("/novel/{novelId}/no/{chapterNo}")
    public Result<Chapter> readByNo(@PathVariable Long novelId, @PathVariable Integer chapterNo) {
        return Result.ok(chapterService.readByNo(novelId, chapterNo));
    }

    @PostMapping
    public Result<Integer> add(@RequestBody Chapter chapter, HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(chapterService.add(chapter));
    }

    @PutMapping
    public Result<Void> update(@RequestBody Chapter chapter, HttpSession session) {
        SessionUtil.requireAdmin(session);
        chapterService.update(chapter);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id, HttpSession session) {
        SessionUtil.requireAdmin(session);
        chapterService.remove(id);
        return Result.ok();
    }

    @GetMapping("/count/{novelId}")
    public Result<Long> count(@PathVariable Long novelId) {
        return Result.ok(chapterService.count(novelId));
    }
}
