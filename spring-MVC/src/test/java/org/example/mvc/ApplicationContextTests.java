package org.example.mvc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 完整容器启动测试：验证所有自定义组件（拦截器、解析器、转换器、视图解析器等）都能正确装配。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApplicationContextTests {

    @Autowired
    private ApplicationContext context;

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertNotNull(context);
        assertTrue(port > 0);
    }

    @Test
    void customBeansRegistered() {
        assertTrue(context.containsBean("requestIdFilter"), "过滤器未注册");
        assertTrue(context.containsBean("myServlet"), "原生 Servlet 未注册");
        assertTrue(context.containsBean("messageSource"), "MessageSource 未注册");
        assertTrue(context.containsBean("localeResolver"), "LocaleResolver 未注册");
        assertTrue(context.containsBean("mvcAsyncExecutor"), "异步线程池未注册");
    }
}
