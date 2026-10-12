package org.example.redis.common;

import lombok.Data;

import java.io.Serializable;

/** 统一响应体。 */
@Data
public class Result<T> implements Serializable {

    private int code;
    private T data;
    private String msg;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.setCode(200);
        r.setData(data);
        r.setMsg("ok");
        return r;
    }

    public static <T> Result<T> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String msg) {
        Result<T> r = new Result<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }

    public static <T> Result<T> fail(String msg) {
        return fail(500, msg);
    }
}
