package org.example.redis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.BizException;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.service.SongService;
import org.example.redis.vo.SongVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 歌曲（用户端）。 */
@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
@Tag(name = "歌曲")
public class SongController {

    private final SongService songService;

    @GetMapping("/list")
    @Operation(summary = "歌曲列表（分类/关键词，走三级缓存）")
    public Result<PageResult<SongVO>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "12") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category) {
        return Result.ok(songService.page(page, size, keyword, category));
    }

    @GetMapping("/hot")
    @Operation(summary = "热歌榜（ZSet）")
    public Result<List<SongVO>> hot(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(songService.hot(limit));
    }

    @GetMapping("/{id}")
    @Operation(summary = "歌曲详情（热点 key：逻辑过期缓存）")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SongVO vo = songService.detail(id);
        if (vo == null) {
            throw new BizException(404, "歌曲不存在或已下架");
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("song", vo);
        return Result.ok(data);
    }

    @PostMapping("/{id}/play")
    @Operation(summary = "播放：计数 + 榜单 + 最近播放 + Stream 流水")
    public Result<Map<String, Object>> play(@PathVariable Long id) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("playCount", songService.play(id));
        return Result.ok(data);
    }

    @PostMapping("/{id}/like")
    @SaCheckLogin
    @Operation(summary = "点赞/取消点赞（Set）")
    public Result<Map<String, Object>> like(@PathVariable Long id) {
        boolean liked = songService.like(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("liked", liked);
        data.put("likeCount", songService.likeCount(id));
        return Result.ok(data);
    }

    @PostMapping("/{id}/favorite")
    @SaCheckLogin
    @Operation(summary = "收藏/取消收藏（Set + DB）")
    public Result<Map<String, Object>> favorite(@PathVariable Long id) {
        boolean favorited = songService.favorite(id);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("favorited", favorited);
        return Result.ok(data);
    }

    @GetMapping("/favorites")
    @SaCheckLogin
    @Operation(summary = "我的收藏")
    public Result<List<SongVO>> favorites() {
        return Result.ok(songService.favorites());
    }

    @GetMapping("/recent")
    @SaCheckLogin
    @Operation(summary = "最近播放（List）")
    public Result<List<SongVO>> recent(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(songService.recent(limit));
    }

    @GetMapping("/common-favorite/{userId}")
    @SaCheckLogin
    @Operation(summary = "和某个用户共同收藏的歌曲（Set 交集）")
    public Result<List<Long>> commonFavorite(@PathVariable Long userId) {
        return Result.ok(songService.commonFavorite(userId));
    }
}
