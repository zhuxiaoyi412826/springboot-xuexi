package org.example.redis.controller.admin;

import cn.dev33.satoken.annotation.SaCheckRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.PageResult;
import org.example.redis.common.Result;
import org.example.redis.entity.Song;
import org.example.redis.service.SongService;
import org.example.redis.vo.SongVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 管理端：歌曲管理（注解式鉴权 @SaCheckRole）。 */
@RestController
@RequestMapping("/api/admin/songs")
@RequiredArgsConstructor
@SaCheckRole("admin")
@Tag(name = "管理端-歌曲")
public class AdminSongController {

    private final SongService songService;

    @GetMapping("/list")
    @Operation(summary = "歌曲列表（含下架）")
    public Result<PageResult<SongVO>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) String keyword) {
        return Result.ok(songService.adminPage(page, size, keyword));
    }

    @PostMapping("/add")
    @Operation(summary = "新增歌曲（同步加入布隆过滤器）")
    public Result<Song> add(@RequestBody Song song) {
        return Result.ok(songService.add(song));
    }

    @PostMapping("/update")
    @Operation(summary = "修改歌曲（同步删除缓存）")
    public Result<Song> update(@RequestBody Song song) {
        return Result.ok(songService.update(song));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除歌曲")
    public Result<String> delete(@PathVariable Long id) {
        songService.delete(id);
        return Result.ok("已删除");
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "上下架（下架后从榜单摘除）")
    public Result<String> status(@PathVariable Long id, @RequestParam int status) {
        songService.setStatus(id, status);
        return Result.ok("已更新");
    }
}
