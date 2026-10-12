package org.example.mybatisplus.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.example.mybatisplus.holder.DataScopeContext;

/**
 * 数据权限：把「普通用户只能看自己的订单」从业务代码里挪到插件里自动拼 SQL。
 *
 * <p>生效范围做了三重限制，避免误伤其它 SQL：
 * <ol>
 *   <li>只对订单 Mapper（BikeOrderMapper）生效</li>
 *   <li>只有 {@link DataScopeContext} 里设置了 userId 才拼接（管理员不设置 → 看全部）</li>
 *   <li>只处理 SELECT（mappedStatementId 里带 select 关键字）</li>
 * </ol>
 *
 * <p>最终 SQL：{@code SELECT ... FROM t_order WHERE (deleted = 0 AND ...) AND user_id = ?}
 */
public class OrderDataPermissionHandler implements DataPermissionHandler {

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        Long userId = DataScopeContext.getUserId();
        if (userId == null) {
            return where;
        }
        if (mappedStatementId == null || !mappedStatementId.contains("BikeOrderMapper")) {
            return where;
        }
        String lower = mappedStatementId.toLowerCase();
        if (!lower.contains("select") && !lower.contains("page")) {
            return where;
        }

        Expression condition = new EqualsTo(new Column("user_id"), new LongValue(userId));
        return where == null ? condition : new AndExpression(where, condition);
    }
}
