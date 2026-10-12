package org.example.mybatis.dto;

import lombok.Data;

/**
 * 用户查询条件（后台用户管理）
 */
@Data
public class UserQuery {

    /** 用户名 / 昵称模糊 */
    private String keyword;

    /** ADMIN / USER */
    private String role;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private String orderBy;
}
