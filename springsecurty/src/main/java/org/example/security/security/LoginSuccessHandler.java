package org.example.security.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.Result;
import org.example.security.entity.User;
import org.example.security.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.DefaultSavedRequest;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录成功处理器：
 * <ul>
 *     <li>页面请求：跳回登录前访问的页面（RequestCache），没有则回首页</li>
 *     <li>XHR 请求：返回 JSON（含角色/权限，方便前端渲染菜单）</li>
 *     <li>顺便更新最后登录时间、清空失败计数</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;

    private final RequestCache requestCache = new HttpSessionRequestCache();
    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String username = authentication.getName();
        loginAttemptService.loginSucceeded(username);

        userRepository.findByUsername(username).ifPresent(user -> {
            user.setLastLoginTime(LocalDateTime.now());
            user.setLastLoginIp(request.getRemoteAddr());
            user.setLoginFailCount(0);
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);
            userRepository.save(user);
        });

        log.info("登录成功：{}（session={}）", username, request.getSession(false) == null ? "无" : request.getSession(false).getId());

        if (WebJson.wantJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("username", username);
            data.put("principal", authentication.getPrincipal() instanceof CustomUserDetails cu ? cu.getNickname() : username);
            data.put("authorities", authentication.getAuthorities().stream().map(a -> a.getAuthority()).toList());
            WebJson.write(response, objectMapper, 200, Result.ok(data));
            return;
        }

        DefaultSavedRequest savedRequest = (DefaultSavedRequest) requestCache.getRequest(request, response);
        String target = savedRequest != null ? savedRequest.getRedirectUrl() : "/user/home.html";
        requestCache.removeRequest(request, response);
        redirectStrategy.sendRedirect(request, response, target);
    }
}
