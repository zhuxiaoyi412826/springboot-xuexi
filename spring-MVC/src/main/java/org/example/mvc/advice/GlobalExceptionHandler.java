package org.example.mvc.advice;

import lombok.extern.slf4j.Slf4j;
import org.example.mvc.common.BizException;
import org.example.mvc.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理（@ControllerAdvice / @RestControllerAdvice）。
 *
 * <p>匹配顺序：先精确（子类）后兜底（Exception）。</p>
 */
@Slf4j
@RestControllerAdvice(basePackages = "org.example.mvc.controller")
public class GlobalExceptionHandler {

    /** 业务异常 -> 400 + 业务码 */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result> handleBiz(BizException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /** @RequestBody 校验失败 / 表单绑定校验失败 -> 400 + 字段级错误明细 */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Result> handleValid(Exception e) {
        BindingResult br = e instanceof MethodArgumentNotValidException m
                ? m.getBindingResult()
                : ((BindException) e).getBindingResult();
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : br.getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        br.getGlobalErrors().forEach(oe -> errors.put(oe.getObjectName(), oe.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Result.fail(400, "参数校验失败：" + errors));
    }

    /** @RequestParam 缺失 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result> handleMissingParam(MissingServletRequestParameterException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "缺少必填参数：" + e.getParameterName()));
    }

    /** 类型转换失败：/mvc/ex/type?num=abc -> int */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest()
                .body(Result.fail(400, "参数类型不匹配：" + e.getName() + " 需要 " + (e.getRequiredType() == null ? "?" : e.getRequiredType().getSimpleName())));
    }

    /** JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result> handleNotReadable(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(Result.fail(400, "请求体不是合法 JSON"));
    }

    /** 上传超限 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result> handleMaxUpload(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(Result.fail(413, "文件超过大小限制"));
    }

    /** ResponseStatusException：直接沿用其中的状态码 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Result> handleResponseStatus(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Result.fail(e.getStatusCode().value(), e.getReason()));
    }

    /** 兜底：返回 RFC 7807 ProblemDetail */
    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<ProblemDetail> handleOthers(Exception e) {
        log.error("未处理异常", e);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        pd.setTitle("服务器内部错误");
        pd.setProperty("tip", "这是 @ExceptionHandler(Exception) 的兜底处理，返回 ProblemDetail");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
