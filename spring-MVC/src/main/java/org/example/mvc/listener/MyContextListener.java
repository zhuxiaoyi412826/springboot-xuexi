package org.example.mvc.listener;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import lombok.extern.slf4j.Slf4j;

/**
 * ServletContext 监听器，通过 ServletListenerRegistrationBean 注册（Java 配置方式）。
 */
@Slf4j
public class MyContextListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        log.info("[MyContextListener] 应用启动，contextPath={}", sce.getServletContext().getContextPath());
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("[MyContextListener] 应用关闭");
    }
}
