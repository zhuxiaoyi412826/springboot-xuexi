package org.example.mybatisplus.holder;

/**
 * 动态表名上下文（ThreadLocal）：演示 DynamicTableNameInnerInterceptor。
 *
 * <p>设置一个表名后，当前线程对 t_order 的所有操作都会落到这张表上（比如按月归档的 t_order_2026）。
 */
public final class DynamicTableContext {

    private static final ThreadLocal<String> TABLE = new ThreadLocal<>();

    private DynamicTableContext() {
    }

    public static void setTable(String tableName) {
        TABLE.set(tableName);
    }

    public static String getTable() {
        return TABLE.get();
    }

    public static void clear() {
        TABLE.remove();
    }
}
