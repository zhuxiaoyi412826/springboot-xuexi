package org.example.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配合自定义 {@code HandlerMethodArgumentResolver} 使用：
 * 标记在参数上后，参数值由 {@code CurrentUserArgumentResolver} 解析（从 Session / 请求属性中获取登录用户）。
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
