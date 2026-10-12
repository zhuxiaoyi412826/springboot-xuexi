package org.example.mybatisplus.controller;

import org.example.mybatisplus.common.Result;
import org.example.mybatisplus.service.LabService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MyBatis-Plus 实验室接口（对应 static/lab.html）。
 *
 * <p>每个接口演示一个 MP 特性，页面上有按钮、下面直接显示返回结果。
 */
@RestController
@RequestMapping("/api/lab")
public class LabController {

    private final LabService labService;

    public LabController(LabService labService) {
        this.labService = labService;
    }

    /** 当前生效的插件清单 */
    @GetMapping("/plugins")
    public Result<Map<String, Object>> plugins() {
        return Result.ok(labService.pluginSummary());
    }

    /** 自动填充：create_time / update_time */
    @PostMapping("/fill")
    public Result<Map<String, Object>> fill() {
        return Result.ok(labService.fillDemo());
    }

    /** 枚举：数据库存中文、Java 用枚举 */
    @PostMapping("/enum")
    public Result<Map<String, Object>> enumDemo() {
        return Result.ok(labService.enumDemo());
    }

    /** 数据权限：普通用户自动带 user_id 条件 */
    @GetMapping("/data-permission")
    public Result<Map<String, Object>> dataPermission(@RequestParam(defaultValue = "2") Long userId) {
        return Result.ok(labService.dataPermissionDemo(userId));
    }

    /** 多租户：切租户写入 + 查询 */
    @PostMapping("/tenant")
    public Result<Map<String, Object>> tenant(@RequestParam(required = false) String name) {
        return Result.ok(labService.tenantDemo(name));
    }

    /** 动态表名：订单写进归档表 */
    @PostMapping("/dynamic-table")
    public Result<Map<String, Object>> dynamicTable(@RequestParam(required = false) String table) {
        return Result.ok(labService.dynamicTableDemo(table));
    }

    /**
     * 并发下单压测：三种并发控制方式对比
     *
     * @param mode atomic / pessimistic / optimistic
     */
    @PostMapping("/concurrent")
    public Result<Map<String, Object>> concurrent(
            @RequestParam(defaultValue = "1") Long bikeId,
            @RequestParam(defaultValue = "20") int threads,
            @RequestParam(defaultValue = "10") int stock,
            @RequestParam(defaultValue = "atomic") String mode) {
        return Result.ok(labService.concurrentOrder(bikeId, threads, stock, mode));
    }

    /** 批量插入三种写法耗时对比 */
    @GetMapping("/batch")
    public Result<Map<String, Object>> batch(@RequestParam(defaultValue = "1000") int n) {
        return Result.ok(labService.batchDemo(n));
    }

    /** 安全防护：不带 where 的 update 会被拦截 */
    @GetMapping("/block-attack")
    public Result<Map<String, Object>> blockAttack() {
        return Result.ok(labService.blockAttackDemo());
    }

    /** 分页 maxLimit：size 再大也被截断 */
    @GetMapping("/max-limit")
    public Result<Map<String, Object>> maxLimit(@RequestParam(defaultValue = "10000") long size) {
        return Result.ok(labService.maxLimitDemo(size));
    }
}
