package org.example.mybatis.common;

import lombok.Getter;

/**
 * 业务异常：由全局异常处理器捕获并转成统一响应
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        super(message);
        this.code = 1;
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }
}
