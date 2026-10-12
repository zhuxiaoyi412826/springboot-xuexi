package org.example.mvc.resolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.mvc.annotation.CurrentUser;
import org.example.mvc.model.User;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 自定义参数解析器：解析 {@code @CurrentUser User user}。
 *
 * <p>自定义解析器排在内置解析器之后、兜底解析器之前，
 * 因此「没有内置注解支持的复杂参数」才会走到这里。</p>
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String SESSION_KEY = "loginUser";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        Object user = null;
        if (request != null) {
            user = request.getAttribute("loginUser"); // 过滤器可以提前放入 request
            if (user == null) {
                HttpSession session = request.getSession(false);
                if (session != null) {
                    user = session.getAttribute(SESSION_KEY);
                }
            }
        }
        if (user instanceof User u) {
            return u;
        }
        // 未登录：返回游客对象（避免 null）
        User guest = new User();
        guest.setId(0L);
        guest.setUsername("guest");
        guest.setNickname("游客");
        return guest;
    }
}
