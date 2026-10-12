package org.example.mybatisplus.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.injector.ISqlInjector;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DynamicTableNameInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.example.mybatisplus.handler.DemoTenantLineHandler;
import org.example.mybatisplus.handler.OrderDataPermissionHandler;
import org.example.mybatisplus.handler.OrderTableHandler;
import org.example.mybatisplus.injector.MySqlInjector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 插件总配置（拦截器链）。
 *
 * <p>执行顺序（自上而下依次改写 SQL）：
 * <ol>
 *   <li>BlockAttackInnerInterceptor：拦截不带 where 的 update/delete（防全表更新删除）</li>
 *   <li>TenantLineInnerInterceptor：多租户，自动拼 tenant_id（本项目只对 t_tenant_goods 生效）</li>
 *   <li>DynamicTableNameInnerInterceptor：动态表名（订单按月/年归档）</li>
 *   <li>DataPermissionInterceptor：行级数据权限（普通用户只看自己的订单）</li>
 *   <li>PaginationInnerInterceptor：分页，maxLimit 防止 size 过大拖库</li>
 *   <li>OptimisticLockerInnerInterceptor：乐观锁，自动带 version 条件并自增</li>
 * </ol>
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 1) 安全防护：禁止「没有 where 条件」的 update / delete
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());

        // 2) 多租户（只对演示表生效，见 DemoTenantLineHandler#ignoreTable）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new DemoTenantLineHandler()));

        // 3) 动态表名：t_order → 上下文里指定的归档表（其它表在 OrderTableHandler 里原样返回）
        interceptor.addInnerInterceptor(new DynamicTableNameInnerInterceptor(new OrderTableHandler()));

        // 4) 数据权限：普通用户只看自己订单
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new OrderDataPermissionHandler()));

        // 5) 分页：单页最多 200 条，超过自动截断，防止 size=999999 拖垮数据库
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(200L);
        pagination.setOptimizeJoin(true);
        interceptor.addInnerInterceptor(pagination);

        // 6) 乐观锁：@Version 字段自动参与 where 并自增
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        return interceptor;
    }

    /** 自定义 SQL 注入器：注入 insertBatchSomeColumn（真正的批量插入） */
    @Bean
    public ISqlInjector sqlInjector() {
        return new MySqlInjector();
    }

    /** 兜底：直接把注入器写进 GlobalConfig，确保 starter 一定用它 */
    @Bean
    public com.baomidou.mybatisplus.autoconfigure.MybatisPlusPropertiesCustomizer mybatisPlusPropertiesCustomizer() {
        return properties -> properties.getGlobalConfig().setSqlInjector(new MySqlInjector());
    }
}
