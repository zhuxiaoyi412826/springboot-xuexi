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
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * OAuth2 / OIDC 登录成功：把第三方账号「落地」成本地用户（首次登录自动注册）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = ((OAuth2AuthenticationToken) authentication).getPrincipal();
        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String login = oAuth2User.getAttribute("login");
        if (login == null || login.isBlank()) {
            login = oAuth2User.getAttribute("name");
        }
        String username = provider + "_" + login;
        String avatar = oAuth2User.getAttribute("avatar_url");
        String email = oAuth2User.getAttribute("email");

        String finalLogin = login;
        User user = userRepository.findByUsername(username).orElseGet(() -> {
            User u = new User();
            u.setUsername(username);
            // 第三方账号没有本地密码，用随机 UUID 生成一份密文占位（永不明文）
            u.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            u.setNickname(finalLogin);
            u.setAvatar(avatar);
            u.setEmail(email);
            u.setSource(provider);
            u.setEnabled(true);
            u.setAccountNonLocked(true);
            u.setLoginFailCount(0);
            u.setCreateTime(LocalDateTime.now());
            u.setRoles(Set.of("ROLE_USER"));
            return userRepository.save(u);
        });
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(request.getRemoteAddr());
        userRepository.save(user);

        log.info("OAuth2 登录成功：{}（provider={}）", username, provider);

        if (WebJson.wantJson(request)) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("username", username);
            data.put("nickname", login);
            data.put("provider", provider);
            data.put("authorities", List.of("ROLE_USER"));
            WebJson.write(response, objectMapper, 200, Result.ok(data));
            return;
        }
        redirectStrategy.sendRedirect(request, response, "/user/home.html");
    }
}
