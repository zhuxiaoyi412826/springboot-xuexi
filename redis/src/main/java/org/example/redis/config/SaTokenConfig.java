package org.example.redis.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 全局路由拦截器（知识点：拦截器式鉴权，区别于注解式）。
 *
 * <p>规则：
 * <ul>
 *   <li>/login.html、静态资源、/api/auth/**、/api/public/**、Knife4j 文档 —— 放行</li>
 *   <li>/admin/\*\*（管理端页面）与 /api/admin/\*\* —— 必须 admin 角色</li>
 *   <li>其余 /api/\*\* —— 必须登录</li>
 * </ul>
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> {
                    // 1) 需要登录的接口
                    SaRouter.match("/api/**")
                            .notMatch(
                                    "/api/auth/**",
                                    "/api/public/**",
                                    "/api/lab/**"
                            )
                            .check(r -> StpUtil.checkLogin());

                    // 2) 管理端页面
                    SaRouter.match("/admin/**").check(r -> StpUtil.checkRole("admin"));

                    // 3) 管理端接口
                    SaRouter.match("/api/admin/**").check(r -> StpUtil.checkRole("admin"));
                }))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/login.html",
                        "/index.html",
                        "/favicon.ico",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/img/**",
                        "/static/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}
