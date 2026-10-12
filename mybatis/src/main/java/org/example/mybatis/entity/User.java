package org.example.mybatis.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户（普通读者 / 后台管理员共用一张表，用 role 区分）
 */
@Data
public class User implements Serializable {

    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String avatar;

    /** ADMIN / USER */
    private String role;

    /** 1 正常 / 0 禁用 */
    private Integer status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer deleted;

    /** CREATE_TIME 的格式化字符串（UserMapper.xml 中用 DATE_FORMAT 查出） */
    private String createTimeStr;

    public boolean isAdmin() {
        return "ADMIN".equals(this.role);
    }
}
