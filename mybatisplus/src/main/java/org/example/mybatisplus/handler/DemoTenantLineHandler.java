package org.example.mybatisplus.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.example.mybatisplus.holder.TenantContext;

/**
 * 多租户：自动给 SQL 拼 tenant_id。
 *
 * <p><b>只对演示表 t_tenant_goods 生效</b>（其余表在 ignoreTable 里返回 true）。
 * 这样既能看到效果，又不会污染已有的 t_user / t_bike / t_order 查询。
 */
public class DemoTenantLineHandler implements TenantLineHandler {

    @Override
    public Expression getTenantId() {
        return new LongValue(TenantContext.getTenant());
    }

    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }

    @Override
    public boolean ignoreTable(String tableName) {
        return !"t_tenant_goods".equalsIgnoreCase(tableName);
    }
}
