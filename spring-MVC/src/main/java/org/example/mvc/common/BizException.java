package org.example.mvc.common;

import lombok.Getter;

/**
 * 业务异常：由 {@code @ControllerAdvice} 中的 {@code @ExceptionHandler} 统一处理。
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        this(1000, msg);
    }
}
