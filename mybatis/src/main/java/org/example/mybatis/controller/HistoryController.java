package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.HistoryVO;
import org.example.mybatis.entity.ReadingHistory;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.HistoryService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 阅读历史 / 书架
 */
@RestController
@RequestMapping("/api/history")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    /**
     * 记录阅读进度
     * 请求体：{ novelId, chapterId, chapterNo, progress }
     */
    @PostMapping
    public Result<Void> record(@RequestBody Map<String, Object> body, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        Long novelId = body.get("novelId") == null ? null : Long.valueOf(body.get("novelId").toString());
        Long chapterId = body.get("chapterId") == null ? null : Long.valueOf(body.get("chapterId").toString());
        Integer chapterNo = body.get("chapterNo") == null ? 1 : Integer.valueOf(body.get("chapterNo").toString());
        Integer progress = body.get("progress") == null ? 0 : Integer.valueOf(body.get("progress").toString());
        if (novelId == null) {
            return Result.fail("缺少 novelId");
        }
        historyService.record(user.getId(), novelId, chapterId, chapterNo, progress);
        return Result.ok();
    }

    /** 我的书架 */
    @GetMapping
    public Result<PageResult<HistoryVO>> my(@RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "12") int pageSize,
                                            HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(historyService.myHistory(user.getId(), pageNum, pageSize));
    }

    /** 某本书的阅读进度 */
    @GetMapping("/{novelId}")
    public Result<ReadingHistory> get(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(historyService.get(user.getId(), novelId));
    }

    /** 移出书架 */
    @DeleteMapping("/{novelId}")
    public Result<Void> remove(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        historyService.remove(user.getId(), novelId);
        return Result.ok();
    }

    @GetMapping("/count")
    public Result<Long> count(HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(historyService.count(user.getId()));
    }
}
