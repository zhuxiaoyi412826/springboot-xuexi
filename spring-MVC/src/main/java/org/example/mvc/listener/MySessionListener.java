package org.example.mvc.listener;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 原生 Servlet 监听器，通过 {@code @WebListener + @ServletComponentScan} 注册（注解方式）。
 * 用来观察在线会话数。
 */
@Slf4j
@WebListener
public class MySessionListener implements HttpSessionListener {

    public static final AtomicInteger ONLINE = new AtomicInteger(0);

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        log.info("[MySessionListener] 会话创建 {}，在线数={}", se.getSession().getId(), ONLINE.incrementAndGet());
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        log.info("[MySessionListener] 会话销毁 {}，在线数={}", se.getSession().getId(), ONLINE.decrementAndGet());
    }
}
