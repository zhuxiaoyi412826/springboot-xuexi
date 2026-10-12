package org.example.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;

/**
 * 方法级安全的表达式处理器：注册自定义 PermissionEvaluator，
 * 让 {@code @PreAuthorize("hasPermission(#id, 'VIDEO', 'delete')")} 这种写法生效。
 */
@Configuration
public class MethodSecurityConfig {

    /** 必须是 static，避免 Bean 过早初始化导致的循环依赖警告 */
    @Bean
    public static MethodSecurityExpressionHandler methodSecurityExpressionHandler(PermissionEvaluator permissionEvaluator) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setPermissionEvaluator(permissionEvaluator);
        return handler;
    }
}
