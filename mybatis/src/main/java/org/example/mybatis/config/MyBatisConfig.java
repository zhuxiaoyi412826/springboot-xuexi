package org.example.mybatis.config;

import org.example.mybatis.plugin.PageInterceptor;
import org.example.mybatis.plugin.SlowSqlInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置
 *
 * <p>MyBatis-Spring-Boot-Starter 会自动收集容器里 {@code Interceptor} 类型的 bean，
 * 并注入到 SqlSessionFactory 的 plugins 中，所以这里只要把插件声明成 bean 即可生效。
 */
@Configuration
public class MyBatisConfig {

    /**
     * 注册物理分页插件
     */
    @Bean
    public PageInterceptor pageInterceptor() {
        return new PageInterceptor();
    }

    /**
     * 注册慢 SQL 监控插件，阈值从配置读取
     */
    @Bean
    public SlowSqlInterceptor slowSqlInterceptor(@Value("${novel.slow-sql-threshold-ms:100}") long thresholdMs) {
        return new SlowSqlInterceptor(thresholdMs);
    }
}
