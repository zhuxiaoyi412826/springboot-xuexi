package org.example.mvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 拦截器：演示 HandlerInterceptor 三个回调方法的执行时机与执行顺序。
 */
@Slf4j
public class LogInterceptor implements HandlerInterceptor, Ordered {

    public static final String START_TIME = "logInterceptor.startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        String handlerName = handler instanceof HandlerMethod hm
                ? hm.getBeanType().getSimpleName() + "#" + hm.getMethod().getName()
                : String.valueOf(handler);
        log.info("[LogInterceptor] preHandle  -> {} {} , handler={}", request.getMethod(), request.getRequestURI(), handlerName);
        return true; // 返回 false 则请求到此为止
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) {
        log.info("[LogInterceptor] postHandle -> model={}", modelAndView == null ? "无" : modelAndView.getModel().keySet());
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        Object start = request.getAttribute(START_TIME);
        long cost = start == null ? -1 : System.currentTimeMillis() - (Long) start;
        log.info("[LogInterceptor] afterCompletion -> status={}, cost={}ms, ex={}", response.getStatus(), cost, ex);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
