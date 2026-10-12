package org.example.redis.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.example.redis.common.Result;
import org.example.redis.service.SignService;
import org.example.redis.service.StatService;
import org.example.redis.service.UserService;
import org.example.redis.vo.UserVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/** 个人中心：资料、签到（Bitmap）、位置（Geo）、统计（UV/日活）。 */
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "个人中心")
public class MeController {

    private final UserService userService;
    private final SignService signService;
    private final StatService statService;

    @Data
    public static class LocationRequest {
        private double lng;
        private double lat;
    }

    @GetMapping("/profile")
    @SaCheckLogin
    @Operation(summary = "我的资料（走三级缓存）")
    public Result<UserVO> profile() {
        return Result.ok(userService.currentUser());
    }

    @PostMapping("/sign")
    @SaCheckLogin
    @Operation(summary = "今日签到（Bitmap）")
    public Result<Map<String, Object>> sign() {
        Long uid = Long.parseLong(StpUtil.getLoginId().toString());
        return Result.ok(signService.sign(uid));
    }

    @GetMapping("/sign")
    @SaCheckLogin
    @Operation(summary = "本月签到情况")
    public Result<Map<String, Object>> signInfo() {
        Long uid = Long.parseLong(StpUtil.getLoginId().toString());
        return Result.ok(signService.monthInfo(uid));
    }

    @PostMapping("/location")
    @SaCheckLogin
    @Operation(summary = "上报位置（Geo）")
    public Result<String> location(@RequestBody LocationRequest req) {
        Long uid = Long.parseLong(StpUtil.getLoginId().toString());
        statService.reportLocation(uid, req.getLng(), req.getLat());
        return Result.ok("已上报");
    }

    @GetMapping("/nearby")
    @Operation(summary = "附近的歌友（GEO 半径查询）")
    public Result<Map<String, Object>> nearby(@RequestParam double lng,
                                              @RequestParam double lat,
                                              @RequestParam(defaultValue = "50") double radiusKm,
                                              @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(statService.nearBy(lng, lat, radiusKm, limit));
    }

    @GetMapping("/stats")
    @Operation(summary = "今日 UV（HyperLogLog）与日活（Bitmap）")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) String day) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("uv", statService.uv(day));
        data.put("active", statService.activeCount(day));
        return Result.ok(data);
    }
}
