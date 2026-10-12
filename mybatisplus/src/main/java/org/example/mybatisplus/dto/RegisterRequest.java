package org.example.mybatisplus.dto;

import lombok.Data;

/**
 * 注册请求参数
 */
@Data
public class RegisterRequest {

    private String username;

    private String password;

    private String nickname;

    private String phone;

    private String address;
}
