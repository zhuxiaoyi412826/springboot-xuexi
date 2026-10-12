package org.example.security.dto;

import lombok.Data;

import java.util.List;

/** 当前登录状态（前端据此渲染菜单） */
@Data
public class AuthStateVO {

    /** 是否已认证（false 时为匿名） */
    private boolean authenticated;
    private String username;
    private String nickname;
    private Long userId;
    private List<String> roles;
    private List<String> authorities;
    /** 认证方式：form / basic / remember-me / oauth2 / api-key / jwt */
    private String authType;
}
