package org.example.security.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.security.security.AuthEventLogger;
import org.example.security.security.SecurityUtil;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话管理：查看在线用户、强制下线、查看/踢掉自己的其它会话。
 *
 * <p>知识点：
 * <ul>
 *     <li>{@link SessionRegistry}：统计在线会话（并发登录控制的基础）</li>
 *     <li>Spring Session（Redis）：{@code findByIndexNameSessionRepository.findByPrincipalName(username)}
 *         直接删除 Session，可实现「多实例部署下强制下线」</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionRegistry sessionRegistry;
    private final AuthEventLogger authEventLogger;
    private final ObjectProvider<FindByIndexNameSessionRepository<? extends Session>> sessionRepositoryProvider;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    /** 在线用户（同一个用户可能有多条会话） */
    public List<Map<String, Object>> onlineUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String username = principal instanceof UserDetails ud ? ud.getUsername() : String.valueOf(principal);
            List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
            for (SessionInformation info : sessions) {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("username", username);
                map.put("sessionId", info.getSessionId());
                map.put("expired", info.isExpired());
                map.put("lastRequest", info.getLastRequest() == null ? "" : FMT.format(Instant.ofEpochMilli(info.getLastRequest().getTime())));
                result.add(map);
            }
        }
        return result;
    }

    /** 强制下线：让该用户所有会话立即失效 */
    public int kick(String username) {
        int count = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String name = principal instanceof UserDetails ud ? ud.getUsername() : String.valueOf(principal);
            if (!username.equals(name)) {
                continue;
            }
            for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                info.expireNow();     // 标记过期，请求时会被并发会话过滤器拦截
                count++;
            }
        }

        // 如果用了 Spring Session(Redis)，直接删除会话记录，多实例同样生效
        FindByIndexNameSessionRepository<? extends Session> repo = sessionRepositoryProvider.getIfAvailable();
        if (repo != null) {
            try {
                Map<String, ? extends Session> sessions = repo.findByPrincipalName(username);
                sessions.forEach((id, session) -> repo.deleteById(id));
                log.info("从会话存储中删除 {} 个会话：{}", sessions.size(), username);
                count = Math.max(count, sessions.size());
            } catch (Exception e) {
                log.warn("删除会话存储记录失败：{}", e.getMessage());
            }
        }

        authEventLogger.write(username, "KICK", true, "管理员强制下线");
        log.warn("强制下线：{}（{} 个会话）", username, count);
        return count;
    }

    /** 我的会话列表 */
    public List<Map<String, Object>> mySessions() {
        String username = SecurityUtil.currentUsername();
        if (username == null) {
            return List.of();
        }
        return onlineUsers().stream().filter(m -> username.equals(m.get("username"))).toList();
    }

    /** 踢掉自己的某个会话（例如「下线其它设备」） */
    public boolean expireMySession(String sessionId) {
        String username = SecurityUtil.currentUsername();
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String name = principal instanceof UserDetails ud ? ud.getUsername() : String.valueOf(principal);
            if (!username.equals(name)) {
                continue;
            }
            for (SessionInformation info : sessionRegistry.getAllSessions(principal, false)) {
                if (info.getSessionId().equals(sessionId)) {
                    info.expireNow();
                    return true;
                }
            }
        }
        return false;
    }
}
