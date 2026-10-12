package org.example.redis.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** 业务异常：401/403/404 等业务码会映射成同名的 HTTP 状态码。 */
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

    public static BizException of(String message) {
        return new BizException(message);
    }
}
