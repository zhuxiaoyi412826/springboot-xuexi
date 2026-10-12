package org.example.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.common.OptimisticConflictException;
import org.example.mybatisplus.dto.OrderRequest;
import org.example.mybatisplus.entity.Bike;
import org.example.mybatisplus.entity.BikeOrder;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.enums.LockMode;
import org.example.mybatisplus.enums.OrderStatus;
import org.example.mybatisplus.holder.DataScopeContext;
import org.example.mybatisplus.mapper.BikeMapper;
import org.example.mybatisplus.mapper.BikeOrderMapper;
import org.example.mybatisplus.mapper.UserMapper;
import org.example.mybatisplus.service.BikeOrderService;
import org.example.mybatisplus.service.BikeStockService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 订单业务：三种并发控制方式 + 数据权限 + 枚举状态。
 */
@Service
public class BikeOrderServiceImpl extends ServiceImpl<BikeOrderMapper, BikeOrder> implements BikeOrderService {

    /**
     * 乐观锁冲突后的最大重试次数。
     * 并发越高需要的重试越多：20 个线程抢同一行时，5 次基本不够，这里给 10 次 + 随机退避。
     */
    private static final int OPTIMISTIC_MAX_RETRY = 10;

    private final BikeMapper bikeMapper;
    private final UserMapper userMapper;
    private final BikeStockService bikeStockService;

    /**
     * 仅用于演示：持有锁后暂停多少毫秒，用来放大并发窗口、观察悲观锁的串行化效果。
     * 默认 0（不暂停），生产保持 0 即可。
     */
    @Value("${bike.order.lock-demo-delay-ms:0}")
    private long lockDemoDelayMs;

    public BikeOrderServiceImpl(BikeMapper bikeMapper, UserMapper userMapper, BikeStockService bikeStockService) {
        this.bikeMapper = bikeMapper;
        this.userMapper = userMapper;
        this.bikeStockService = bikeStockService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BikeOrder createOrder(Long userId, OrderRequest request) {
        return doCreateOrder(userId, request, LockMode.ATOMIC);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BikeOrder createOrder(Long userId, OrderRequest request, LockMode mode) {
        return doCreateOrder(userId, request, mode == null ? LockMode.ATOMIC : mode);
    }

    /**
     * 下单核心逻辑。
     *
     * <p>注意：本方法是 private，事务由上面 public 方法的 @Transactional 开启，
     * 保证悲观锁的 FOR UPDATE 与后续扣减、插入订单处在同一个事务里。
     */
    private BikeOrder doCreateOrder(Long userId, OrderRequest request, LockMode mode) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        if (request == null || request.getBikeId() == null) {
            throw new BizException("请选择要购买的自行车");
        }
        int quantity = (request.getQuantity() == null || request.getQuantity() < 1) ? 1 : request.getQuantity();

        Bike bike;
        switch (mode) {
            case PESSIMISTIC -> bike = deductByPessimisticLock(request.getBikeId(), quantity);
            case OPTIMISTIC -> bike = deductByOptimisticLock(request.getBikeId(), quantity);
            default -> bike = deductByAtomicUpdate(request.getBikeId(), quantity);
        }

        BikeOrder order = new BikeOrder();
        // 订单号用 MP 自带的 IdWorker 生成 32 位 UUID，避免时间戳 + 随机数在并发下撞号
        order.setOrderNo("BK" + com.baomidou.mybatisplus.core.toolkit.IdWorker.get32UUID());
        order.setUserId(user.getId());
        order.setUsername(user.getUsername());
        order.setBikeId(bike.getId());
        order.setBikeName(bike.getName());
        order.setPrice(bike.getPrice());
        order.setQuantity(quantity);
        order.setTotalAmount(bike.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setReceiver(StringUtils.hasText(request.getReceiver()) ? request.getReceiver() : user.getNickname());
        order.setPhone(StringUtils.hasText(request.getPhone()) ? request.getPhone() : user.getPhone());
        order.setAddress(StringUtils.hasText(request.getAddress()) ? request.getAddress() : user.getAddress());
        // 枚举状态（数据库存「待发货」）
        order.setStatus(OrderStatus.WAIT);
        // createTime / updateTime 由 MyMetaObjectHandler 自动填充，这里不再手写
        this.save(order);
        return order;
    }

    // ==================== 三种扣库存方式 ====================

    /**
     * 方式一：原子 UPDATE（无锁，推荐）
     * {@code UPDATE t_bike SET stock = stock - n WHERE id = ? AND stock >= n}
     * 库存不够时影响行数为 0，天然不会超卖。
     */
    private Bike deductByAtomicUpdate(Long bikeId, int quantity) {
        Bike bike = checkAvailable(bikeId, quantity);
        int rows = bikeMapper.update(null, Wrappers.<Bike>lambdaUpdate()
                .eq(Bike::getId, bike.getId())
                .ge(Bike::getStock, quantity)
                .setSql("stock = stock - " + quantity));
        if (rows == 0) {
            throw new BizException("库存不足，请刷新后重试");
        }
        return bike;
    }

    /**
     * 方式二：悲观锁（SELECT ... FOR UPDATE）
     * 必须在事务内（见 doCreateOrder 上的 @Transactional），否则锁会立刻释放。
     */
    private Bike deductByPessimisticLock(Long bikeId, int quantity) {
        Bike bike = bikeMapper.selectByIdForUpdate(bikeId);
        if (bike == null) {
            throw new BizException("自行车不存在");
        }
        checkBikeStatus(bike, quantity);
        sleepForDemo();
        bike.setStock(bike.getStock() - quantity);
        bikeMapper.updateById(bike);
        return bike;
    }

    /**
     * 方式三：乐观锁（@Version）
     *
     * <p>每次尝试都交给 {@link org.example.mybatisplus.service.BikeStockService}，
     * 它是 REQUIRES_NEW 的新事务，能读到最新的 version（否则在可重复读下重试读到的还是旧快照）。
     * 版本冲突就退避后重试，库存不足/下架这类错误直接抛出，不重试。
     */
    private Bike deductByOptimisticLock(Long bikeId, int quantity) {
        for (int i = 0; i < OPTIMISTIC_MAX_RETRY; i++) {
            try {
                return bikeStockService.deductOptimistic(bikeId, quantity);
            } catch (OptimisticConflictException conflict) {
                // 版本冲突：随机退避后再重试，避免一堆线程同时重试又同时冲突
                try {
                    Thread.sleep(10 + ThreadLocalRandom.current().nextInt(40));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new BizException("下单被中断");
                }
            }
        }
        throw new BizException("并发太激烈，乐观锁重试 " + OPTIMISTIC_MAX_RETRY + " 次仍未成功，请稍后再试");
    }

    // ==================== 校验 ====================

    private Bike checkAvailable(Long bikeId, int quantity) {
        Bike bike = bikeMapper.selectById(bikeId);
        if (bike == null) {
            throw new BizException("自行车不存在");
        }
        checkBikeStatus(bike, quantity);
        return bike;
    }

    private void checkBikeStatus(Bike bike, int quantity) {
        if (bike.getStatus() == null || bike.getStatus() != 1) {
            throw new BizException("该商品已下架");
        }
        if (bike.getStock() == null || bike.getStock() < quantity) {
            throw new BizException("库存不足，当前库存：" + (bike.getStock() == null ? 0 : bike.getStock()));
        }
    }

    private void sleepForDemo() {
        if (lockDemoDelayMs > 0) {
            try {
                Thread.sleep(lockDemoDelayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException("下单被中断");
            }
        }
    }

    // ==================== 查询 ====================

    /**
     * 我的订单：这里故意<b>不写</b> {@code eq(userId)}，
     * 而是把 userId 放进 {@link DataScopeContext}，由 DataPermissionInterceptor 自动拼 {@code user_id = ?}。
     */
    @Override
    public IPage<BikeOrder> pageMyOrders(Long userId, long current, long size) {
        try {
            DataScopeContext.setUserId(userId);
            return this.page(new Page<>(current, size), Wrappers.<BikeOrder>lambdaQuery()
                    .orderByDesc(BikeOrder::getId));
        } finally {
            DataScopeContext.clear();
        }
    }

    /** 管理员：不设置数据权限上下文 → 看全部 */
    @Override
    public IPage<BikeOrder> pageAllOrders(long current, long size, OrderStatus status, String keyword) {
        DataScopeContext.clear();
        LambdaQueryWrapper<BikeOrder> wrapper = Wrappers.lambdaQuery();
        if (status != null) {
            wrapper.eq(BikeOrder::getStatus, status);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(BikeOrder::getOrderNo, kw)
                    .or().like(BikeOrder::getUsername, kw)
                    .or().like(BikeOrder::getBikeName, kw));
        }
        wrapper.orderByDesc(BikeOrder::getId);
        return this.page(new Page<>(current, size), wrapper);
    }

    /** 状态统计：QueryWrapper 的 select + groupBy（非 Lambda 写法，适合原生 SQL 片段） */
    @Override
    public List<Map<String, Object>> statByStatus() {
        DataScopeContext.clear();
        return baseMapper.selectMaps(new QueryWrapper<BikeOrder>()
                .select("status AS status, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS amount")
                .groupBy("status"));
    }

    // ==================== 状态流转 / 取消 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long orderId, OrderStatus status) {
        if (status == null) {
            throw new BizException("订单状态不合法");
        }
        BikeOrder order = this.getById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (OrderStatus.CANCEL.equals(order.getStatus())) {
            throw new BizException("已取消的订单不能修改状态");
        }
        order.setStatus(status);
        this.updateById(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, Long userId, boolean admin) {
        BikeOrder order = this.getById(orderId);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (!admin && !order.getUserId().equals(userId)) {
            throw new BizException("只能取消自己的订单");
        }
        if (OrderStatus.CANCEL.equals(order.getStatus())) {
            throw new BizException("订单已取消，请勿重复操作");
        }
        if (OrderStatus.DONE.equals(order.getStatus())) {
            throw new BizException("已完成的订单不能取消");
        }
        // 归还库存
        bikeMapper.update(null, Wrappers.<Bike>lambdaUpdate()
                .eq(Bike::getId, order.getBikeId())
                .setSql("stock = stock + " + order.getQuantity()));
        order.setStatus(OrderStatus.CANCEL);
        this.updateById(order);
    }
}
