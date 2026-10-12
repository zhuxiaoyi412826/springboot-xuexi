package org.example.redis.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.Result;
import org.example.redis.service.SearchService;
import org.example.redis.vo.SongVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 搜索（热搜榜 ZSet + 历史 List + 结果缓存）。 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "搜索")
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @Operation(summary = "搜索歌曲")
    public Result<List<SongVO>> search(@RequestParam String q,
                                       @RequestParam(defaultValue = "20") int limit) {
        return Result.ok(searchService.search(q, limit));
    }

    @GetMapping("/hot")
    @Operation(summary = "热搜榜（ZSet 带分数）")
    public Result<List<Map<String, Object>>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(searchService.hotSearch(limit));
    }

    @GetMapping("/history")
    @Operation(summary = "搜索历史（List）")
    public Result<List<String>> history() {
        return Result.ok(searchService.history());
    }

    @DeleteMapping("/history")
    @Operation(summary = "清空搜索历史")
    public Result<String> clearHistory() {
        searchService.clearHistory();
        return Result.ok("已清空");
    }
}
