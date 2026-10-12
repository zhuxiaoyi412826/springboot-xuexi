package org.example.mvc.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 统一响应体：用于演示 @ResponseBody / HttpMessageConverter / 自定义返回值处理器等。
 */
@Getter
@Setter
public class Result implements Serializable {

    private int code;
    private String msg;
    private Object data;

    public static Result ok() {
        return ok(null);
    }

    public static Result ok(Object data) {
        Result r = new Result();
        r.setCode(0);
        r.setMsg("ok");
        r.setData(data);
        return r;
    }

    public static Result fail(int code, String msg) {
        Result r = new Result();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
