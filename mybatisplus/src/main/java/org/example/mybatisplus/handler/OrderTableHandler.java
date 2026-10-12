package org.example.mybatisplus.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import org.example.mybatisplus.holder.DynamicTableContext;

/**
 * 动态表名：只作用在 t_order 上，用于演示「订单归档」。
 *
 * <p>注意：3.5.17 的 DynamicTableNameInnerInterceptor 收的是单个全局 handler，
 * 所以"只改哪张表"要在这里自己判断表名，其它表原样返回。
 * 没设置 {@link DynamicTableContext} 时也原样返回，日常业务不受影响。
 */
public class OrderTableHandler implements TableNameHandler {

    @Override
    public String dynamicTableName(String sql, String tableName) {
        if (!"t_order".equalsIgnoreCase(tableName)) {
            return tableName;
        }
        String target = DynamicTableContext.getTable();
        return (target == null || target.isBlank()) ? tableName : target;
    }
}
