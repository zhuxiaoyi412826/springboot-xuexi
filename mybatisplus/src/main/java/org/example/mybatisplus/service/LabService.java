package org.example.mybatisplus.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.dto.OrderRequest;
import org.example.mybatisplus.entity.BatchDemo;
import org.example.mybatisplus.entity.Bike;
import org.example.mybatisplus.entity.BikeOrder;
import org.example.mybatisplus.entity.TenantGoods;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.enums.LockMode;
import org.example.mybatisplus.enums.OrderStatus;
import org.example.mybatisplus.holder.DataScopeContext;
import org.example.mybatisplus.holder.DynamicTableContext;
import org.example.mybatisplus.holder.TenantContext;
import org.example.mybatisplus.mapper.BatchDemoMapper;
import org.example.mybatisplus.mapper.BikeOrderMapper;
import org.example.mybatisplus.mapper.TenantGoodsMapper;
import org.example.mybatisplus.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 「MyBatis-Plus 实验室」的业务实现：把每个插件的效果做成可以直接点的接口。
 */
@Service
public class LabService {

    private final BikeOrderService orderService;
    private final BikeOrderMapper orderMapper;
    private final BikeService bikeService;
    private final UserMapper userMapper;
    private final TenantGoodsMapper tenantGoodsMapper;
    private final BatchDemoMapper batchDemoMapper;
    private final BatchDemoService batchDemoService;

    public LabService(BikeOrderService orderService, BikeOrderMapper orderMapper, BikeService bikeService,
                      UserMapper userMapper, TenantGoodsMapper tenantGoodsMapper, BatchDemoMapper batchDemoMapper,
                      BatchDemoService batchDemoService) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
        this.bikeService = bikeService;
        this.userMapper = userMapper;
        this.tenantGoodsMapper = tenantGoodsMapper;
        this.batchDemoMapper = batchDemoMapper;
        this.batchDemoService = batchDemoService;
    }

    // ==================== 1. 三种并发控制方式压测 ====================

    /**
     * 并发下单压测：先把库存重置成 stock，再用 threads 个线程同时抢，
     * 三种模式（atomic/pessimistic/optimistic）都不会超卖，但失败数/耗时差别明显。
     */
    public Map<String, Object> concurrentOrder(Long bikeId, int threads, int stock, String mode) {
        if (threads < 1 || threads > 200) {
            throw new BizException("线程数请控制在 1~200");
        }
        Bike bike = bikeService.getById(bikeId);
        if (bike == null) {
            throw new BizException("商品不存在");
        }
        // 重置库存 + 恢复在售，保证每次实验结果可复现
        bikeService.update(Wrappers.<Bike>lambdaUpdate()
                .eq(Bike::getId, bikeId)
                .set(Bike::getStock, stock)
                .set(Bike::getStatus, 1));

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getRole, "USER").last("LIMIT 1"));
        if (user == null) {
            throw new BizException("请先初始化演示数据（需要一个普通用户）");
        }

        LockMode lockMode = LockMode.of(mode);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger fail = new AtomicInteger();
        List<String> errors = new ArrayList<>();
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(threads, 50));
        CountDownLatch latch = new CountDownLatch(threads);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    OrderRequest req = new OrderRequest();
                    req.setBikeId(bikeId);
                    req.setQuantity(1);
                    orderService.createOrder(user.getId(), req, lockMode);
                    success.incrementAndGet();
                } catch (Exception e) {
                    fail.incrementAndGet();
                    synchronized (errors) {
                        if (errors.size() < 3) {
                            errors.add(e.getMessage());
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        try {
            latch.await(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        pool.shutdown();
        long cost = System.currentTimeMillis() - start;

        Bike after = bikeService.getById(bikeId);
        Map<String, Object> result = new HashMap<>();
        result.put("mode", lockMode.getCode());
        result.put("modeText", lockMode.getText());
        result.put("threads", threads);
        result.put("stockBefore", stock);
        result.put("success", success.get());
        result.put("fail", fail.get());
        result.put("stockAfter", after == null ? null : after.getStock());
        result.put("version", after == null ? null : after.getVersion());
        result.put("costMs", cost);
        result.put("errors", errors);
        result.put("tip", "三种模式都不会超卖：库存最小到 0；区别在失败数与耗时");
        return result;
    }

    // ==================== 2. 自动填充 ====================

    /** 不传 createTime/updateTime 新增一条商品，看 MP 自动回填 */
    public Map<String, Object> fillDemo() {
        Bike bike = new Bike();
        bike.setName("自动填充测试车-" + System.currentTimeMillis());
        bike.setBrand("MP 实验室");
        bike.setCategory("城市车");
        bike.setPrice(new BigDecimal("1999.00"));
        bike.setStock(10);
        bike.setStatus(1);
        bike.setDescription("MP 实验室：create_time / update_time 由 MetaObjectHandler 自动填充");
        bikeService.saveOrUpdate(bike);

        Bike saved = bikeService.getById(bike.getId());
        // 改一次价格，验证 update_time 自动更新
        saved.setPrice(new BigDecimal("1888.00"));
        bikeService.updateById(saved);
        Bike updated = bikeService.getById(bike.getId());

        Map<String, Object> map = new HashMap<>();
        map.put("id", updated.getId());
        map.put("createTime", updated.getCreateTime());
        map.put("updateTime", updated.getUpdateTime());
        map.put("price", updated.getPrice());
        map.put("tip", "两个时间都不是手写 set 进去的；改价格后 updateTime 自动刷新");
        // 演示完删掉（逻辑删除）
        bikeService.removeById(updated.getId());
        return map;
    }

    // ==================== 3. 枚举 ====================

    /** 下单 → 改状态，看数据库存的是中文、Java 里是枚举、JSON 里是枚举名 */
    public Map<String, Object> enumDemo() {
        Bike bike = bikeService.list(Wrappers.<Bike>lambdaQuery()
                .eq(Bike::getStatus, 1).ge(Bike::getStock, 1).last("LIMIT 1")).stream().findFirst().orElse(null);
        if (bike == null) {
            throw new BizException("没有可售商品");
        }
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getRole, "USER").last("LIMIT 1"));
        if (user == null) {
            throw new BizException("请先初始化演示数据");
        }
        OrderRequest req = new OrderRequest();
        req.setBikeId(bike.getId());
        req.setQuantity(1);
        BikeOrder order = orderService.createOrder(user.getId(), req, LockMode.ATOMIC);

        orderService.updateStatus(order.getId(), OrderStatus.SENT);
        BikeOrder sent = orderService.getById(order.getId());

        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", sent.getOrderNo());
        map.put("statusEnumName", sent.getStatus() == null ? null : sent.getStatus().name());
        map.put("statusDbValue", sent.getStatusText());
        map.put("statusAfter", sent.getStatus() == null ? null : sent.getStatus().name());
        map.put("tip", "Java=枚举 / 数据库=中文 / JSON=枚举名，全靠 @EnumValue + MybatisEnumTypeHandler");
        return map;
    }

    // ==================== 4. 数据权限 ====================

    /**
     * 对比「普通用户」和「管理员」查订单时 SQL 的差异。
     * 普通用户：DataPermissionInterceptor 自动拼 user_id = ?
     * 管理员：不拼条件，看全部
     */
    public Map<String, Object> dataPermissionDemo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        boolean admin = user.isAdmin();

        Map<String, Object> map = new HashMap<>();
        map.put("username", user.getUsername());
        map.put("role", user.getRole());

        // 用 count 对比最直观：同一个 selectCount(null)，有没有数据权限差出来的就是别人的订单
        Long asUser;
        DataScopeContext.setUserId(admin ? null : user.getId());
        try {
            asUser = orderMapper.selectCount(null);
        } finally {
            DataScopeContext.clear();
        }

        DataScopeContext.clear();
        long all = orderMapper.selectCount(null);

        map.put("withDataPermission", asUser);
        map.put("withoutDataPermission", all);
        map.put("filtered", !admin);
        map.put("tip", admin ? "管理员：不拼 user_id，看到全部订单" : "普通用户：插件自动拼上 user_id 条件");
        return map;
    }

    // ==================== 5. 多租户 ====================

    /** 两个租户各写一条商品，再各自查询——互不看见 */
    public Map<String, Object> tenantDemo(String name) {
        Map<String, Object> map = new HashMap<>();

        TenantContext.setTenant(1L);
        try {
            TenantGoods g1 = new TenantGoods();
            g1.setName((name == null ? "商品" : name) + "-租户A");
            g1.setPrice(new BigDecimal("1000.00"));
            g1.setStock(10);
            tenantGoodsMapper.insert(g1);
            map.put("tenant1Count", tenantGoodsMapper.selectCount(null));
        } finally {
            TenantContext.clear();
        }

        TenantContext.setTenant(2L);
        try {
            TenantGoods g2 = new TenantGoods();
            g2.setName((name == null ? "商品" : name) + "-租户B");
            g2.setPrice(new BigDecimal("2000.00"));
            g2.setStock(20);
            tenantGoodsMapper.insert(g2);
            List<TenantGoods> list = tenantGoodsMapper.selectList(Wrappers.<TenantGoods>lambdaQuery().last("LIMIT 10"));
            map.put("tenant2Count", tenantGoodsMapper.selectCount(null));
            map.put("tenant2Names", list.stream().map(TenantGoods::getName).toList());
        } finally {
            TenantContext.clear();
        }

        map.put("tip", "插入不写 tenant_id、查询不写 where tenant_id，全由 TenantLineInnerInterceptor 处理");
        return map;
    }

    // ==================== 6. 动态表名 ====================

    /** 把订单写进归档表 t_order_2026 再读回来（同一套实体和 Mapper） */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> dynamicTableDemo(String table) {
        String target = (table == null || table.isBlank()) ? "t_order_2026" : table.trim();
        Map<String, Object> map = new HashMap<>();
        map.put("table", target);

        DynamicTableContext.setTable(target);
        try {
            BikeOrder order = new BikeOrder();
            order.setOrderNo("ARCH" + System.currentTimeMillis());
            order.setUserId(1L);
            order.setUsername("归档演示");
            order.setBikeId(1L);
            order.setBikeName("归档订单");
            order.setPrice(new BigDecimal("1.00"));
            order.setQuantity(1);
            order.setTotalAmount(new BigDecimal("1.00"));
            order.setStatus(OrderStatus.DONE);
            orderMapper.insert(order);

            List<BikeOrder> list = orderMapper.selectList(Wrappers.<BikeOrder>lambdaQuery().last("LIMIT 5"));
            map.put("archivedCount", list.size());
            map.put("orderNos", list.stream().map(BikeOrder::getOrderNo).toList());
        } finally {
            DynamicTableContext.clear();
        }

        map.put("mainTableCount", orderMapper.selectCount(null));
        map.put("tip", "同一套 BikeOrderMapper，切了上下文就写到归档表；主表数据不受影响");
        return map;
    }

    // ==================== 7. 批量插入三种写法对比 ====================

    public Map<String, Object> batchDemo(int n) {
        if (n < 1 || n > 20000) {
            throw new BizException("条数请控制在 1~20000");
        }

        // ① 逐条 insert：n 次网络往返
        // 注意：不能写 delete(null)，会被 BlockAttackInnerInterceptor 判定为「全表删除」拦截
        clearBatchDemo();
        long t1 = System.currentTimeMillis();
        for (BatchDemo demo : buildList(n)) {
            batchDemoMapper.insert(demo);
        }
        long loopCost = System.currentTimeMillis() - t1;

        // ② saveBatch：JDBC Batch（要在 JDBC URL 上加 rewriteBatchedStatements=true）
        // 注意：不能写 delete(null)，会被 BlockAttackInnerInterceptor 判定为「全表删除」拦截
        clearBatchDemo();
        long t2 = System.currentTimeMillis();
        batchDemoService.saveBatch(buildList(n), 1000);
        long saveBatchCost = System.currentTimeMillis() - t2;

        // ③ insertBatchSomeColumn：一条 INSERT 多组 VALUES
        // 注意：不能写 delete(null)，会被 BlockAttackInnerInterceptor 判定为「全表删除」拦截
        clearBatchDemo();
        long t3 = System.currentTimeMillis();
        batchDemoMapper.insertBatchSomeColumn(buildList(n));
        long batchSqlCost = System.currentTimeMillis() - t3;

        Map<String, Object> map = new HashMap<>();
        map.put("rows", n);
        map.put("loopInsertMs", loopCost);
        map.put("saveBatchMs", saveBatchCost);
        map.put("insertBatchSomeColumnMs", batchSqlCost);
        map.put("fastest", min(loopCost, saveBatchCost, batchSqlCost));
        map.put("tip", "insertBatchSomeColumn 只有一条 SQL，通常最快；差距会随条数变大");
        // 注意：不能写 delete(null)，会被 BlockAttackInnerInterceptor 判定为「全表删除」拦截
        clearBatchDemo();
        return map;
    }

    /** 清空压测表（带 where 条件，避免触发 BlockAttackInnerInterceptor） */
    private void clearBatchDemo() {
        batchDemoMapper.delete(Wrappers.<BatchDemo>lambdaQuery().ge(BatchDemo::getId, 0L));
    }

    private List<BatchDemo> buildList(int n) {
        List<BatchDemo> list = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            BatchDemo demo = new BatchDemo();
            demo.setName("压测商品-" + i);
            demo.setPrice(new BigDecimal("99.00"));
            list.add(demo);
        }
        return list;
    }

    private String min(long loop, long saveBatch, long batchSql) {
        long m = Math.min(loop, Math.min(saveBatch, batchSql));
        if (m == loop) {
            return "loopInsert";
        }
        return m == saveBatch ? "saveBatch" : "insertBatchSomeColumn";
    }

    // ==================== 8. 安全防护 ====================

    /** 演示 BlockAttackInnerInterceptor：不带 where 的 update 会被直接拒绝 */
    public Map<String, Object> blockAttackDemo() {
        Map<String, Object> map = new HashMap<>();
        try {
            // 故意不带任何条件：会被插件拦截
            bikeService.update(Wrappers.<Bike>lambdaUpdate().set(Bike::getStock, 999));
            map.put("blocked", false);
            map.put("tip", "没被拦截（检查 BlockAttackInnerInterceptor 是否注册）");
        } catch (Exception e) {
            map.put("blocked", true);
            map.put("message", e.getMessage());
            map.put("tip", "全表更新被拦截：必须带 where 条件");
        }
        return map;
    }

    /** 演示分页 maxLimit：请求 size=10000，实际只返回 maxLimit 条 */
    public Map<String, Object> maxLimitDemo(long size) {
        Page<Bike> page = new Page<>(1, size);
        bikeService.page(page, Wrappers.<Bike>lambdaQuery().eq(Bike::getStatus, 1));
        Map<String, Object> map = new HashMap<>();
        map.put("requestSize", size);
        map.put("actualSize", page.getSize());
        map.put("records", page.getRecords().size());
        map.put("total", page.getTotal());
        map.put("tip", "PaginationInnerInterceptor.setMaxLimit(200)：再大的 size 也会被截断");
        return map;
    }

    /** 当前生效的插件清单（教学用） */
    public Map<String, Object> pluginSummary() {
        Map<String, Object> map = new HashMap<>();
        map.put("BlockAttackInnerInterceptor", "拦截无 where 的 update/delete");
        map.put("TenantLineInnerInterceptor", "多租户（仅 t_tenant_goods）");
        map.put("DynamicTableNameInnerInterceptor", "动态表名（仅 t_order）");
        map.put("DataPermissionInterceptor", "行级数据权限（订单：普通用户只看自己）");
        map.put("PaginationInnerInterceptor", "分页，maxLimit=200");
        map.put("OptimisticLockerInnerInterceptor", "乐观锁 @Version（t_bike.version）");
        map.put("MySqlInjector", "注入 insertBatchSomeColumn");
        map.put("MyMetaObjectHandler", "自动填充 create_time / update_time");
        map.put("MybatisEnumTypeHandler", "枚举 ↔ 数据库中文值");
        return map;
    }
}
