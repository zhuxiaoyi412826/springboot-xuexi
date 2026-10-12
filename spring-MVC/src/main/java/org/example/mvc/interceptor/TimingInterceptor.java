package org.example.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 异步拦截器：AsyncHandlerInterceptor 比 HandlerInterceptor 多一个 afterConcurrentHandlingStarted，
 * 在 Controller 返回 Callable / DeferredResult 等异步结果、主线程释放后立即回调。
 */
@Slf4j
public class TimingInterceptor implements AsyncHandlerInterceptor, Ordered {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("[TimingInterceptor] preHandle 线程={}", Thread.currentThread().getName());
        return true;
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        log.info("[TimingInterceptor] 异步开始，主线程已释放：{}", Thread.currentThread().getName());
    }

    @Override
    public int getOrder() {
        return 3;
    }
}
