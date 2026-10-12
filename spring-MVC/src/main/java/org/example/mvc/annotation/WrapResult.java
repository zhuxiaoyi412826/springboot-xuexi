package org.example.mvc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 配合自定义 {@code HandlerMethodReturnValueHandler} 使用：
 * 标记在类或方法上后，返回值会被自动包装成统一的 Result JSON。
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface WrapResult {
}
