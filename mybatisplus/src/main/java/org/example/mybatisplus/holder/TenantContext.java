package org.example.mybatisplus.holder;

/**
 * 多租户上下文（ThreadLocal）：演示 TenantLineInnerInterceptor 自动拼 tenant_id。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT = new ThreadLocal<>();

    /** 默认租户：总部（用于没显式设置租户时的请求） */
    public static final long DEFAULT_TENANT = 1L;

    private TenantContext() {
    }

    public static void setTenant(Long tenantId) {
        TENANT.set(tenantId);
    }

    public static Long getTenant() {
        Long id = TENANT.get();
        return id == null ? DEFAULT_TENANT : id;
    }

    public static void clear() {
        TENANT.remove();
    }
}
