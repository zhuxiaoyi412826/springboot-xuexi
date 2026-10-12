package org.example.security.controller;

import lombok.RequiredArgsConstructor;
import org.example.security.common.Result;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 列出「当前启用了哪些第三方登录」，登录页据此动态渲染按钮。
 */
@RestController
@RequestMapping("/app/auth")
@RequiredArgsConstructor
public class OAuth2ProvidersController {

    private final ObjectProvider<ClientRegistrationRepository> repositoryProvider;

    @GetMapping("/oauth2-clients")
    public Result<List<Map<String, String>>> clients() {
        ClientRegistrationRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            return Result.ok(List.of());
        }
        List<Map<String, String>> list = new java.util.ArrayList<>();
        // InMemoryClientRegistrationRepository 实现了 Iterable<ClientRegistration>
        if (repository instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                if (!(item instanceof ClientRegistration registration)) {
                    continue;
                }
                Map<String, String> map = new LinkedHashMap<>();
                map.put("registrationId", registration.getRegistrationId());
                map.put("clientName", registration.getClientName());
                map.put("loginUrl", "/oauth2/authorization/" + registration.getRegistrationId());
                list.add(map);
            }
        }
        return Result.ok(list);
    }
}
