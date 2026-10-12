package org.example.security.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

/**
 * 知识点：OAuth2 资源服务器（校验 JWT）。
 *
 * <p>真实环境里 JWT 由认证中心（Keycloak / Auth0 / 自建 Authorization Server）签发，
 * 资源服务只配置 issuer-uri 或 public key 来验签。
 * 为了让本项目「开箱可玩」，这里在启动时生成一对 RSA 密钥：
 * <ul>
 *     <li>{@code POST /api/auth/token} 用私钥签发 JWT（扮演认证中心）</li>
 *     <li>{@code /api/**} 资源服务器用公钥验签</li>
 * </ul>
 * 生产环境应替换为 {@code spring.security.oauth2.resourceserver.jwt.issuer-uri}。</p>
 */
@Configuration
public class JwtConfig {

    /** RSA 密钥对（重启会更换，生产环境应从配置/密钥管理服务加载） */
    @Bean
    public KeyPair jwtKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    /** 签发 JWT（JwtEncoder） */
    @Bean
    public JwtEncoder jwtEncoder(KeyPair keyPair) {
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        JWKSource<SecurityContext> jwkSource = (jwkSelector, context) -> jwkSelector.select(jwkSet);
        return new NimbusJwtEncoder(jwkSource);
    }

    /** 验签 JWT（JwtDecoder） */
    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {
        return NimbusJwtDecoder.withPublicKey((RSAPublicKey) keyPair.getPublic()).build();
    }
}
