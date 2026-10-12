package org.example.security.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 业务层异常处理（注意：认证/授权异常不在这里处理，
 * 它们由 Spring Security 的 AuthenticationEntryPoint / AccessDeniedHandler 处理）。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常：HTTP 状态码优先取业务码本身（401/403/404 等业务码要保持语义），
     * 无法解析或非 4xx 的业务码统一回落到 400。
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> biz(BizException e) {
        HttpStatus resolved = HttpStatus.resolve(e.getCode());
        HttpStatus status = (resolved != null && resolved.is4xxClientError()) ? resolved : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> valid(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : e.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(Result.fail(400, "参数校验失败：" + errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> others(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.fail(500, "服务器内部错误：" + e.getMessage()));
    }
}
