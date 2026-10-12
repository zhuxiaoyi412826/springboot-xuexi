package org.example.mybatisplus.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.spring.service.IService;
import org.example.mybatisplus.dto.OrderRequest;
import org.example.mybatisplus.entity.BikeOrder;
import org.example.mybatisplus.enums.LockMode;
import org.example.mybatisplus.enums.OrderStatus;

import java.util.List;
import java.util.Map;

public interface BikeOrderService extends IService<BikeOrder> {

    /**
     * 用户下单：默认用原子 UPDATE 扣库存
     */
    BikeOrder createOrder(Long userId, OrderRequest request);

    /**
     * 用户下单：指定并发控制方式
     *
     * @param mode {@link LockMode}：atomic / pessimistic / optimistic
     */
    BikeOrder createOrder(Long userId, OrderRequest request, LockMode mode);

    /**
     * 我的订单（不写 eq(userId)，由 DataPermissionInterceptor 自动拼 user_id 条件）
     */
    IPage<BikeOrder> pageMyOrders(Long userId, long current, long size);

    /**
     * 后台：查询所有订单（可按状态过滤）
     */
    IPage<BikeOrder> pageAllOrders(long current, long size, OrderStatus status, String keyword);

    /**
     * 修改订单状态（枚举）
     */
    void updateStatus(Long orderId, OrderStatus status);

    /**
     * 取消订单（归还库存）
     */
    void cancelOrder(Long orderId, Long userId, boolean admin);

    /**
     * 订单状态统计：演示 QueryWrapper 的 select + groupBy（返回 status -> 数量）
     */
    List<Map<String, Object>> statByStatus();
}
