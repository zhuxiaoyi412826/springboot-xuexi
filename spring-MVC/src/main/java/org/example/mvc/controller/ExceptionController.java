package org.example.mvc.controller;

import org.example.mvc.common.BizException;
import org.example.mvc.common.Result;
import org.example.mvc.common.SpecialException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 异常处理演示：不同异常走不同的处理链路。
 */
@RestController
@RequestMapping("/mvc/ex")
public class ExceptionController {

    /** 业务异常 -> @ExceptionHandler(BizException) -> 400 */
    @GetMapping("/biz")
    public Result biz() {
        throw new BizException(1001, "演示业务异常（BizException）");
    }

    /** ResponseStatusException -> 沿用其中的 404 */
    @GetMapping("/status")
    public Result status() {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "资源不存在（ResponseStatusException）");
    }

    /** 类型不匹配 -> MethodArgumentTypeMismatchException（访问 /mvc/ex/type?num=abc） */
    @GetMapping("/type")
    public Result type(@RequestParam int num) {
        return Result.ok(num);
    }

    /** 缺少必填参数 -> MissingServletRequestParameterException */
    @GetMapping("/missing")
    public Result missing(@RequestParam String token) {
        return Result.ok(token);
    }

    /** 由自定义 HandlerExceptionResolver 处理（不走 @ControllerAdvice） */
    @GetMapping("/special")
    public Result special() {
        throw new SpecialException("该异常被自定义 HandlerExceptionResolver 处理，返回 409");
    }

    /** 兜底：返回 RFC 7807 ProblemDetail */
    @GetMapping("/error")
    public Result error() {
        throw new IllegalStateException("未预期异常（走 @ExceptionHandler(Exception) 兜底）");
    }
}
