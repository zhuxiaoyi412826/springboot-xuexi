package org.example.security.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务异常（由 ControllerAdvice 统一转成 JSON） */
@Data
@EqualsAndHashCode(callSuper = false)
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BizException(String message) {
        this(400, message);
    }
}
