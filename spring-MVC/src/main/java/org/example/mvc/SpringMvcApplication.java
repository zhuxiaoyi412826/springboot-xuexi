package org.example.mvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启动类。
 *
 * <p>{@code @ServletComponentScan}：扫描 {@code @WebServlet / @WebFilter / @WebListener}，
 * 这是「原生 Servlet 组件」接入 Spring Boot 的两种方式之一（另一种是 XXXRegistrationBean）。</p>
 */
@EnableScheduling
@ServletComponentScan
@SpringBootApplication
public class SpringMvcApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringMvcApplication.class, args);
    }
}
