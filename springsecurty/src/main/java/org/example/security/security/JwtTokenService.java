package org.example.security.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 签发 JWT（模拟认证中心）。
 *
 * <p>载荷里放了两类权限：roles（角色）与 perms（细粒度权限），
 * 资源服务器用 JwtAuthenticationConverter 把它们还原成 GrantedAuthority。</p>
 */
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.ttl-minutes}")
    private long ttlMinutes;

    public String issue(UserDetails user, Long userId) {
        Instant now = Instant.now();
        List<String> all = user.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        List<String> roles = all.stream().filter(a -> a.startsWith("ROLE_")).toList();
        List<String> perms = all.stream().filter(a -> !a.startsWith("ROLE_")).toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getUsername())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlMinutes * 60))
                .claim("uid", userId)
                .claim("roles", roles)
                .claim("perms", perms)
                .build();

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long ttlSeconds() {
        return ttlMinutes * 60;
    }
}
