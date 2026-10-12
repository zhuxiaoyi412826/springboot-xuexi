package org.example.mybatisplus.holder;

/**
 * 数据权限上下文（ThreadLocal）。
 *
 * <p>用法：在 Service 里 setUserId(id) → 之后所有 t_order 查询会被
 * {@link org.example.mybatisplus.handler.OrderDataPermissionHandler} 自动拼上 {@code user_id = ?}，
 * 用完必须 clear()，否则会串到同线程的下一个请求（线程池复用）。
 */
public final class DataScopeContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private DataScopeContext() {
    }

    /** 设置「只能看这个用户的订单」；传 null 表示不限（管理员看全部） */
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
    }
}
