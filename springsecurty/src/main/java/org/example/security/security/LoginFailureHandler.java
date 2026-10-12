package org.example.security.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.common.Result;
import org.example.security.entity.User;
import org.example.security.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 登录失败处理器：
 * <ul>
 *     <li>区分失败原因（密码错误 / 账号禁用 / 账号锁定），给出不同提示</li>
 *     <li>失败计数（Redis）+ 达到阈值锁定账号（知识点：防暴力破解）</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final org.springframework.beans.factory.ObjectProvider<org.springframework.core.env.Environment> environment;

    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        String username = request.getParameter("username");
        String msg = message(exception);

        if (username != null && !username.isBlank() && exception instanceof BadCredentialsException) {
            long failed = loginAttemptService.loginFailed(username);
            int max = 5;
            int lockMinutes = 10;
            var env = environment.getIfAvailable();
            if (env != null) {
                max = env.getProperty("app.login.max-attempts", Integer.class, 5);
                lockMinutes = env.getProperty("app.login.lock-minutes", Integer.class, 10);
            }
            if (failed >= max) {
                lockUser(username, lockMinutes);
                msg = "连续失败 " + failed + " 次，账号已锁定 " + lockMinutes + " 分钟";
            } else {
                msg = msg + "，还可尝试 " + (max - failed) + " 次";
            }
        }

        log.warn("登录失败：{} -> {}", username, exception.getMessage());

        if (WebJson.wantJson(request)) {
            WebJson.write(response, objectMapper, 401, Result.unauthorized(msg));
            return;
        }
        redirectStrategy.sendRedirect(request, response, "/user/login.html?error=" + java.net.URLEncoder.encode(msg, java.nio.charset.StandardCharsets.UTF_8));
    }

    private void lockUser(String username, int minutes) {
        userRepository.findByUsername(username).ifPresent(user -> {
            user.setAccountNonLocked(false);
            user.setLockedUntil(LocalDateTime.now().plusMinutes(minutes));
            userRepository.save(user);
        });
    }

    private String message(AuthenticationException e) {
        if (e instanceof DisabledException) {
            return "账号已被禁用，请联系管理员";
        }
        if (e instanceof LockedException) {
            return "账号已被锁定，请稍后再试";
        }
        if (e instanceof BadCredentialsException) {
            return "用户名或密码错误";
        }
        return e.getMessage();
    }

    /** 供定时任务/登录时调用：解锁过期的账号 */
    public void unlockIfExpired(User user) {
        if (Boolean.FALSE.equals(user.getAccountNonLocked())
                && user.getLockedUntil() != null
                && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);
            user.setLoginFailCount(0);
        }
    }
}
