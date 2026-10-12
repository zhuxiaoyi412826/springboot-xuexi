package org.example.redis.common;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理。
 * 知识点：Sa-Token 的登录/角色/权限异常需要转成 401 / 403，而不是 500。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotLoginException.class)
    public Result<Void> notLogin(NotLoginException e) {
        return Result.fail(401, "未登录或登录已过期：" + e.getMessage());
    }

    @ExceptionHandler(NotRoleException.class)
    public Result<Void> notRole(NotRoleException e) {
        return Result.fail(403, "需要角色：" + e.getRole() + "（当前不具备）");
    }

    @ExceptionHandler(NotPermissionException.class)
    public Result<Void> notPermission(NotPermissionException e) {
        return Result.fail(403, "需要权限：" + e.getCode());
    }

    @ExceptionHandler(BizException.class)
    public Result<Void> biz(BizException e) {
        HttpStatus resolved = HttpStatus.resolve(e.getCode());
        int status = (resolved != null && resolved.is4xxClientError()) ? e.getCode() : 400;
        return Result.fail(status, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> others(Exception e) {
        log.error("未处理异常", e);
        return Result.fail(500, "服务器内部错误：" + e.getMessage());
    }
}
