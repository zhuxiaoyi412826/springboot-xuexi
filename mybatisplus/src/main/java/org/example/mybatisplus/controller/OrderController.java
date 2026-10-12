package org.example.mybatisplus.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpSession;
import org.example.mybatisplus.common.Result;
import org.example.mybatisplus.common.SessionUtil;
import org.example.mybatisplus.dto.OrderRequest;
import org.example.mybatisplus.entity.BikeOrder;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.enums.LockMode;
import org.example.mybatisplus.enums.OrderStatus;
import org.example.mybatisplus.service.BikeOrderService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 订单：普通用户下单 / 查看我的订单；管理员查看全部订单、改状态、删除
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final BikeOrderService orderService;

    public OrderController(BikeOrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * 下单（登录用户）
     *
     * @param lock 并发控制方式：
     *             atomic = 原子 UPDATE（默认）、pessimistic = 悲观锁、optimistic = 乐观锁
     */
    @PostMapping
    public Result<BikeOrder> create(
            @RequestBody OrderRequest request,
            @RequestParam(required = false, defaultValue = "atomic") String lock,
            HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(orderService.createOrder(user.getId(), request, LockMode.of(lock)));
    }

    /**
     * 我的订单（数据权限：SQL 里的 user_id 条件由 DataPermissionInterceptor 自动拼）
     */
    @GetMapping("/my")
    public Result<IPage<BikeOrder>> myOrders(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        return Result.ok(orderService.pageMyOrders(user.getId(), current, size));
    }

    /** 全部订单（管理员） */
    @GetMapping
    public Result<IPage<BikeOrder>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(orderService.pageAllOrders(current, size, OrderStatus.of(status), keyword));
    }

    /** 订单状态统计（管理员）：QueryWrapper 的 select + groupBy */
    @GetMapping("/stat")
    public Result<List<Map<String, Object>>> stat(HttpSession session) {
        SessionUtil.requireAdmin(session);
        return Result.ok(orderService.statByStatus());
    }

    /**
     * 修改订单状态（管理员）
     * 传枚举名（WAIT/SENT/DONE/CANCEL）或中文（待发货/已发货/…）都可以
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam String status, HttpSession session) {
        SessionUtil.requireAdmin(session);
        OrderStatus target = OrderStatus.of(status);
        if (target == null) {
            return Result.fail("订单状态不合法");
        }
        orderService.updateStatus(id, target);
        return Result.ok();
    }

    /** 取消订单：管理员可取消任意订单，普通用户只能取消自己的 */
    @DeleteMapping("/{id}")
    public Result<Void> cancel(@PathVariable Long id, HttpSession session) {
        User user = SessionUtil.requireLogin(session);
        orderService.cancelOrder(id, user.getId(), user.isAdmin());
        return Result.ok();
    }
}
