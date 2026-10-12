package org.example.mybatis.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

/**
 * 慢 SQL 监控插件
 *
 * <p>同样是拦截 Executor#query，用环绕的方式统计耗时，超过阈值就 WARN 告警。
 * 这类插件在实际项目里常用于：慢 SQL 排查、SQL 审计、性能埋点。
 */
@Slf4j
@Intercepts(@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class SlowSqlInterceptor implements Interceptor {

    /** 慢 SQL 阈值（毫秒） */
    private final long thresholdMs;

    public SlowSqlInterceptor() {
        this(500);
    }

    public SlowSqlInterceptor(long thresholdMs) {
        this.thresholdMs = thresholdMs;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long start = System.currentTimeMillis();
        try {
            return invocation.proceed();
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (cost > thresholdMs) {
                MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
                log.warn("【慢SQL】耗时 {}ms > 阈值 {}ms，mapper={}", cost, thresholdMs, ms.getId());
            }
        }
    }
}
