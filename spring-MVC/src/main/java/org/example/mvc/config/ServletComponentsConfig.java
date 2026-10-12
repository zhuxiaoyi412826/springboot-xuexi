package org.example.mvc.config;

import org.example.mvc.filter.RequestIdFilter;
import org.example.mvc.filter.RequestLogFilter;
import org.example.mvc.listener.MyContextListener;
import org.example.mvc.servlet.MyServlet;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 原生 Servlet 三大组件的「Java 配置注册方式」：
 * ServletRegistrationBean / FilterRegistrationBean / ServletListenerRegistrationBean。
 *
 * <p>另一种方式是 @WebXxx 注解 + @ServletComponentScan（见 MySessionListener）。</p>
 */
@Configuration
public class ServletComponentsConfig {

    /** 普通 Servlet，映射 /my-servlet，order 越小越优先 */
    @Bean
    public ServletRegistrationBean<MyServlet> myServlet() {
        ServletRegistrationBean<MyServlet> bean = new ServletRegistrationBean<>(new MyServlet(), "/my-servlet");
        bean.setLoadOnStartup(1);
        bean.setName("myServlet");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> bean = new FilterRegistrationBean<>(new RequestIdFilter());
        bean.setUrlPatterns(List.of("/*"));
        bean.setOrder(1);
        bean.setName("requestIdFilter");
        return bean;
    }

    @Bean
    public FilterRegistrationBean<RequestLogFilter> requestLogFilter() {
        FilterRegistrationBean<RequestLogFilter> bean = new FilterRegistrationBean<>(new RequestLogFilter());
        bean.setUrlPatterns(List.of("/*"));
        bean.setOrder(2);
        bean.setName("requestLogFilter");
        return bean;
    }

    @Bean
    public ServletListenerRegistrationBean<MyContextListener> myContextListener() {
        return new ServletListenerRegistrationBean<>(new MyContextListener());
    }
}
