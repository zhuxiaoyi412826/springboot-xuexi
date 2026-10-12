package org.example.security.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

/**
 * 自定义 LogoutHandler：在销毁会话前记录审计日志、清理业务缓存等。
 *
 * <p>知识点：logout 由多个 LogoutHandler + 一个 LogoutSuccessHandler 组成，
 * 默认已经包含 SecurityContextLogoutHandler（清 SecurityContext）与记住我清理。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditLogoutHandler implements LogoutHandler {

    private final AuthEventLogger authEventLogger;

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        if (authentication != null) {
            log.info("清理会话：用户={} session={}", authentication.getName(),
                    request.getSession(false) == null ? "无" : request.getSession(false).getId());
        }
    }
}
