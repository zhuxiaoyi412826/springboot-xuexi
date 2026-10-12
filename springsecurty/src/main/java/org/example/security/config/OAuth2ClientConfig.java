package org.example.security.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

/**
 * 知识点：OAuth2 / OIDC 第三方登录。
 *
 * <p>只有配置了 client-id 才会注册 GitHub 登录（否则启动时会因「client id 为空」失败）。
 * 启动方式：
 * <pre>
 *   java -jar app.jar --app.oauth2.github.client-id=xxx --app.oauth2.github.client-secret=yyy
 * </pre>
 * 回调地址默认是 {@code /login/oauth2/code/github}，需要在 GitHub 应用里配置成
 * {@code http://localhost:8084/login/oauth2/code/github}。</p>
 */
@Slf4j
@Configuration
public class OAuth2ClientConfig {

    /** 只有 app.oauth2.github.client-id 非空时才注册（空字符串会导致 OAuth2 自动配置启动失败） */
    @Bean
    @org.springframework.boot.autoconfigure.condition.ConditionalOnExpression("'${app.oauth2.github.client-id:}' != ''")
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.github.client-id}") String clientId,
            @Value("${app.oauth2.github.client-secret}") String clientSecret) {

        ClientRegistration github = ClientRegistration.withRegistrationId("github")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("read:user")
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("login")
                .clientName("GitHub")
                .build();

        log.info("已启用 GitHub OAuth2 登录，回调地址：/login/oauth2/code/github");
        return new InMemoryClientRegistrationRepository(github);
    }
}
