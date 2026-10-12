package org.example.mybatisplus.common;

/**
 * 业务异常：会被全局异常处理器转成 Result.fail(msg)
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
