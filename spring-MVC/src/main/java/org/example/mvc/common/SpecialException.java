package org.example.mvc.common;

/**
 * 特殊异常：不走 @ControllerAdvice，而是由自定义 {@code HandlerExceptionResolver} 处理，
 * 用来演示「异常解析器链」中自定义解析器的优先级与生效方式。
 */
public class SpecialException extends RuntimeException {

    public SpecialException(String msg) {
        super(msg);
    }
}
