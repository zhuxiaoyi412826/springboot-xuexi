package org.example.mybatis.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.example.mybatis.plugin.PageContext.Page;

import java.util.List;

/**
 * 自研物理分页插件（这就是 PageHelper 的核心原理）
 *
 * <p>MyBatis 插件四大对象：Executor、ParameterHandler、ResultSetHandler、StatementHandler。
 * 本插件拦截 {@link Executor#query}，在真正执行前做两件事：
 * <ol>
 *   <li>把原 SQL 包一层执行一次 COUNT，算出 total 写回 {@link PageContext}</li>
 *   <li>在原 SQL 后追加 {@code LIMIT offset, size}，实现物理分页（不是内存分页）</li>
 * </ol>
 *
 * <p>只对调用了 {@code PageContext.start()} 之后的第一次查询生效。
 */
@Slf4j
@Intercepts(@Signature(
        type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}))
public class PageInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Page page = PageContext.get();
        // 没有开启分页，原样放行
        if (page == null) {
            return invocation.proceed();
        }
        // consumed：保证只对本线程紧接着的一次查询生效（COUNT 查询本身要放行）
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];
        Executor executor = (Executor) invocation.getTarget();
        BoundSql boundSql = ms.getBoundSql(parameter);

        // 1) 查询总数
        long total = queryTotal(executor, ms, parameter, boundSql);
        page.setTotal(total);

        // 2) 追加 LIMIT，构造新的 MappedStatement
        String pageSql = boundSql.getSql() + " LIMIT " + page.getOffset() + "," + page.getPageSize();
        BoundSql pageBoundSql = copyBoundSql(ms, boundSql, pageSql, parameter);
        invocation.getArgs()[0] = copyMappedStatement(ms, wrapSqlSource(pageBoundSql));

        return invocation.proceed();
    }

    /** 用 COUNT 包裹原 SQL 查询总数 */
    private long queryTotal(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql) throws Exception {
        String countSql = "SELECT COUNT(*) FROM (" + boundSql.getSql() + ") tmp_count";
        BoundSql countBoundSql = copyBoundSql(ms, boundSql, countSql, parameter);
        // COUNT 必须用「单一 Long 列」的结果映射，不能沿用原 statement 的 resultMap
        MappedStatement countMs = copyMappedStatementForCount(ms, wrapSqlSource(countBoundSql));
        CacheKey cacheKey = executor.createCacheKey(countMs, parameter, RowBounds.DEFAULT, countBoundSql);
        List<Object> res = executor.query(countMs, parameter, RowBounds.DEFAULT, Executor.NO_RESULT_HANDLER,
                cacheKey, countBoundSql);
        Object first = (res == null || res.isEmpty()) ? null : res.get(0);
        return first == null ? 0L : ((Number) first).longValue();
    }

    /**
     * 为 COUNT 查询构造独立的 MappedStatement：结果类型是单个 Long
     */
    private MappedStatement copyMappedStatementForCount(MappedStatement ms, SqlSource sqlSource) {
        org.apache.ibatis.session.Configuration cfg = ms.getConfiguration();
        org.apache.ibatis.mapping.ResultMap countResultMap = new org.apache.ibatis.mapping.ResultMap.Builder(
                cfg, ms.getId() + "_CountInline", Long.class, new java.util.ArrayList<>(), null).build();

        MappedStatement.Builder builder = new MappedStatement.Builder(
                cfg, ms.getId() + "_Count", sqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.statementType(ms.getStatementType());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(java.util.Collections.singletonList(countResultMap));
        builder.resultSetType(ms.getResultSetType());
        builder.flushCacheRequired(false);
        builder.useCache(false);
        builder.databaseId(ms.getDatabaseId());
        return builder.build();
    }

    /** 复制 BoundSql（保留参数映射与动态产生的附加参数） */
    private BoundSql copyBoundSql(MappedStatement ms, BoundSql source, String sql, Object parameter) {
        BoundSql target = new BoundSql(ms.getConfiguration(), sql, source.getParameterMappings(), parameter);
        source.getParameterMappings().forEach(mapping -> {
            String prop = mapping.getProperty();
            if (source.hasAdditionalParameter(prop)) {
                target.setAdditionalParameter(prop, source.getAdditionalParameter(prop));
            }
        });
        return target;
    }

    private SqlSource wrapSqlSource(BoundSql boundSql) {
        return (parameterObject) -> boundSql;
    }

    /** 基于原 MappedStatement 复制出一个新的（改了 SQL，其它属性保持一致） */
    private MappedStatement copyMappedStatement(MappedStatement ms, SqlSource sqlSource) {
        MappedStatement.Builder builder = new MappedStatement.Builder(
                ms.getConfiguration(), ms.getId() + "_Page", sqlSource, ms.getSqlCommandType());
        builder.resource(ms.getResource());
        builder.fetchSize(ms.getFetchSize());
        builder.statementType(ms.getStatementType());
        builder.keyGenerator(ms.getKeyGenerator());
        builder.timeout(ms.getTimeout());
        builder.parameterMap(ms.getParameterMap());
        builder.resultMaps(ms.getResultMaps());
        builder.resultSetType(ms.getResultSetType());
        builder.cache(ms.getCache());
        builder.flushCacheRequired(ms.isFlushCacheRequired());
        builder.useCache(ms.isUseCache());
        builder.databaseId(ms.getDatabaseId());
        return builder.build();
    }
}
