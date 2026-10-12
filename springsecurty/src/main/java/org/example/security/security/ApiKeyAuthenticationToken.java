package org.example.security.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/** 自定义认证令牌：API Key（演示「自定义认证方式」） */
public class ApiKeyAuthenticationToken extends AbstractAuthenticationToken {

    private final String apiKey;
    private final Object principal;

    /** 未认证状态（由过滤器构造后交给 AuthenticationManager） */
    @SuppressWarnings("unchecked")
    public ApiKeyAuthenticationToken(String apiKey) {
        super((java.util.Collection<GrantedAuthority>) null);
        this.apiKey = apiKey;
        this.principal = null;
        setAuthenticated(false);
    }

    /** 已认证状态（由 ApiKeyAuthenticationProvider 返回） */
    public ApiKeyAuthenticationToken(Object principal, String apiKey,
                                     Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.apiKey = apiKey;
        this.principal = principal;
        super.setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return apiKey;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
