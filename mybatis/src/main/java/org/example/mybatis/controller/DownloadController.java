package org.example.mybatis.controller;

import jakarta.servlet.http.HttpSession;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.common.Result;
import org.example.mybatis.common.SessionUtil;
import org.example.mybatis.dto.DownloadTaskVO;
import org.example.mybatis.dto.TaskQuery;
import org.example.mybatis.entity.User;
import org.example.mybatis.service.DownloadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 下载任务：下载 / 暂停 / 继续 / 停止
 */
@RestController
@RequestMapping("/api/downloads")
public class DownloadController {

    private final DownloadService downloadService;

    public DownloadController(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    /** 创建并开始下载 */
    @PostMapping("/create/{novelId}")
    public Result<DownloadTaskVO> create(@PathVariable Long novelId, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(downloadService.create(user.getId(), novelId));
    }

    /** 我的下载任务（可按状态筛选） */
    @GetMapping
    public Result<PageResult<DownloadTaskVO>> page(@ModelAttribute TaskQuery query,
                                                   @RequestParam(defaultValue = "1") int pageNum,
                                                   @RequestParam(defaultValue = "10") int pageSize,
                                                   HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        query.setUserId(user.getId());
        return Result.ok(downloadService.page(query, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    public Result<DownloadTaskVO> get(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(downloadService.get(id));
    }

    /** 暂停 */
    @PutMapping("/{id}/pause")
    public Result<Void> pause(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        downloadService.pause(user.getId(), id);
        return Result.ok();
    }

    /** 继续 */
    @PutMapping("/{id}/resume")
    public Result<Void> resume(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        downloadService.resume(user.getId(), id);
        return Result.ok();
    }

    /** 停止 */
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        downloadService.cancel(user.getId(), id);
        return Result.ok();
    }

    /** 全部停止（批量，XML foreach） */
    @PutMapping("/cancel-all")
    public Result<Void> cancelAll(HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        downloadService.cancelAll(user.getId());
        return Result.ok();
    }

    /** 重新下载 */
    @PostMapping("/{id}/restart")
    public Result<DownloadTaskVO> restart(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(downloadService.restart(user.getId(), id));
    }

    /** 各状态任务数 */
    @GetMapping("/stat")
    public Result<Map<String, Object>> stat(HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(downloadService.statusStat());
    }

    @GetMapping("/stat/group")
    public Result<List<Map<String, Object>>> group(HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(downloadService.countGroupByStatus());
    }
}
