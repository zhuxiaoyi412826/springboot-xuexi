package org.example.mvc.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.example.mvc.common.Result;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自定义 /error 处理（实现 ErrorController 会顶替 Boot 默认的 BasicErrorController）。
 * 用于观察「容器错误页 -> /error 转发」这条链路。
 */
@RestController
public class MyErrorController implements ErrorController {

    @RequestMapping("/error")
    public Result error(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object uri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        int code = status == null ? 500 : Integer.parseInt(String.valueOf(status));
        return Result.fail(code, "错误页：status=" + code + ", message=" + message + ", uri=" + uri);
    }
}
