package org.example.mvc.config;

import org.example.mvc.advice.MyHandlerExceptionResolver;
import org.example.mvc.convert.MoneyFormatter;
import org.example.mvc.convert.StringToDeptConverter;
import org.example.mvc.http.UserTextHttpMessageConverter;
import org.example.mvc.interceptor.LogInterceptor;
import org.example.mvc.interceptor.LoginInterceptor;
import org.example.mvc.interceptor.TimingInterceptor;
import org.example.mvc.resolver.CurrentUserArgumentResolver;
import org.example.mvc.resolver.WrapResultReturnValueHandler;
import org.example.mvc.view.StaticViewResolver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.ViewResolverRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.http.converter.HttpMessageConverter;

import jakarta.servlet.MultipartConfigElement;

import java.time.Duration;
import java.util.List;

/**
 * Spring MVC 的「总配置入口」：WebMvcConfigurer 的常用回调几乎都在这里。
 *
 * <p>每个方法对应一个可扩展点，注释里标明了它影响 Spring MVC 的哪个组件。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${mvc.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${mvc.async-timeout:8000}")
    private long asyncTimeout;

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final WrapResultReturnValueHandler wrapResultReturnValueHandler;
    private final MyHandlerExceptionResolver myHandlerExceptionResolver;
    private final AsyncTaskExecutor mvcAsyncExecutor;

    public WebMvcConfig(CurrentUserArgumentResolver currentUserArgumentResolver,
                        WrapResultReturnValueHandler wrapResultReturnValueHandler,
                        MyHandlerExceptionResolver myHandlerExceptionResolver,
                        @Qualifier("mvcAsyncExecutor") AsyncTaskExecutor mvcAsyncExecutor) {
        this.currentUserArgumentResolver = currentUserArgumentResolver;
        this.wrapResultReturnValueHandler = wrapResultReturnValueHandler;
        this.myHandlerExceptionResolver = myHandlerExceptionResolver;
        this.mvcAsyncExecutor = mvcAsyncExecutor;
    }

    /* ---------------- 1. 类型转换 / 格式化 ---------------- */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new StringToDeptConverter());   // 全局 Converter
        registry.addFormatterForFieldType(java.math.BigDecimal.class, new MoneyFormatter()); // 全局 Formatter
    }

    /* ---------------- 2. 拦截器 ---------------- */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LogInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new TimingInterceptor()).addPathPatterns("/**");
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/mvc/secure/**")
                .excludePathPatterns("/mvc/secure/public");
        // ?lang=en_US 切换语言
        LocaleChangeInterceptor localeChangeInterceptor = new LocaleChangeInterceptor();
        localeChangeInterceptor.setParamName("lang");
        registry.addInterceptor(localeChangeInterceptor).addPathPatterns("/**");
    }

    /* ---------------- 3. 静态资源 ---------------- */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(Duration.ofHours(1)));
        // 上传目录映射成可直接访问的资源
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }

    /* ---------------- 4. CORS ---------------- */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/mvc/cors/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /* ---------------- 5. 简单视图控制器（不用写 Controller） ---------------- */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("redirect:/index.html");
        registry.addViewController("/home").setViewName("static:hello");
    }

    /* ---------------- 6. 视图解析器 ---------------- */
    @Override
    public void configureViewResolvers(ViewResolverRegistry registry) {
        registry.viewResolver(new StaticViewResolver());
    }

    /* ---------------- 7. 自定义参数解析器 ---------------- */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    /* ---------------- 8. 自定义返回值处理器 ---------------- */
    @Override
    public void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> handlers) {
        handlers.add(wrapResultReturnValueHandler);
    }

    /* ---------------- 9. 消息转换器 ---------------- */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, new UserTextHttpMessageConverter());
    }

    /* ---------------- 10. 异步支持 ---------------- */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(asyncTimeout);
        configurer.setTaskExecutor(mvcAsyncExecutor);
    }

    /* ---------------- 11. 异常解析器（追加，不替换） ---------------- */
    @Override
    public void extendHandlerExceptionResolvers(List<HandlerExceptionResolver> resolvers) {
        resolvers.add(0, myHandlerExceptionResolver);
    }

    /* ---------------- 12. 默认 Servlet 兜底（注释掉的原因见下） ----------------
     * configurer.enable() 会把未映射请求交给容器默认 Servlet，
     * 但嵌入式 Tomcat 中该 Servlet 名为 "default" 且在此时尚未注册，会抛
     * "Unable to locate the default servlet for serving static content"，
     * 因此这里不使用；WebMvcConfigurer 中保留注释以便演示该扩展点。
     *
     * @Override
     * public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
     *     configurer.enable("default");
     * }
     */

    /* ---------------- 13. 上传大小限制（不依赖配置文件 key 的版本差异） ---------------- */
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(org.springframework.util.unit.DataSize.ofMegabytes(5));
        factory.setMaxRequestSize(org.springframework.util.unit.DataSize.ofMegabytes(20));
        return factory.createMultipartConfig();
    }
}
