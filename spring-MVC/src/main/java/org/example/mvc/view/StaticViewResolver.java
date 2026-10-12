package org.example.mvc.view;

import org.springframework.core.Ordered;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

/**
 * 自定义视图解析器：视图名以 {@code static:} 开头时，映射到 /pages/xxx.html。
 * 例：return "static:hello";  ->  /pages/hello.html
 */
public class StaticViewResolver implements ViewResolver, Ordered {

    public static final String PREFIX = "static:";

    @Override
    public View resolveViewName(String viewName, Locale locale) {
        if (viewName == null || !viewName.startsWith(PREFIX)) {
            return null; // 返回 null 表示交给下一个 ViewResolver
        }
        String name = viewName.substring(PREFIX.length());
        return new StaticView(name.startsWith("/") ? name : "/pages/" + name + ".html");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
