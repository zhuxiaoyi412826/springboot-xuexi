package org.example.security.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.entity.LoginLog;
import org.example.security.repository.LoginLogRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 认证事件审计：登录成功 / 失败 / 退出 写入 login_log 表。
 *
 * <p>知识点：Spring Security 会把认证过程发布成 ApplicationEvent，
 * 用 @EventListener 即可无侵入地做审计、风控、消息通知。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventLogger {

    private final LoginLogRepository loginLogRepository;

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        write(username, "LOGIN_SUCCESS", true, "登录成功");
        log.info("[审计] {} 登录成功", username);
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication() == null ? "未知" : String.valueOf(event.getAuthentication().getPrincipal());
        String reason = event.getException() == null ? "" : event.getException().getMessage();
        write(username, "LOGIN_FAILURE", false, reason);
        log.warn("[审计] {} 登录失败：{}", username, reason);
    }

    @EventListener
    public void onLogout(LogoutSuccessEvent event) {
        String username = event.getAuthentication() == null ? "未知" : event.getAuthentication().getName();
        write(username, "LOGOUT", true, "退出登录");
    }

    /** 供「强制下线」调用 */
    public void write(String username, String type, boolean success, String message) {
        try {
            String ip = "本地";
            String ua = "";
            var attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                ip = sra.getRequest().getRemoteAddr();
                ua = sra.getRequest().getHeader("User-Agent");
            }
            LoginLog logEntity = new LoginLog();
            logEntity.setUsername(username);
            logEntity.setIp(ip);
            logEntity.setUserAgent(ua == null ? "" : (ua.length() > 250 ? ua.substring(0, 250) : ua));
            logEntity.setType(type);
            logEntity.setSuccess(success);
            logEntity.setMessage(message);
            logEntity.setCreateTime(LocalDateTime.now());
            loginLogRepository.save(logEntity);
        } catch (Exception e) {
            log.warn("写入登录日志失败：{}", e.getMessage());
        }
    }
}
