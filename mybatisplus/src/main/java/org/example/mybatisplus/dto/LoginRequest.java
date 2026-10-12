package org.example.mybatisplus.dto;

import lombok.Data;

/**
 * 登录请求参数
 */
@Data
public class LoginRequest {

    private String username;

    private String password;
}
