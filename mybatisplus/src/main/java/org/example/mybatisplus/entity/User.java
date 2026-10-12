package org.example.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户（普通用户 + 后台管理员）
 */
@Data
@TableName("t_user")
public class User {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_USER = "USER";

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录账号 */
    private String username;

    /** 登录密码 */
    private String password;

    /** 昵称 */
    private String nickname;

    /** 手机号 */
    private String phone;

    /** 收货地址 */
    private String address;

    /** 角色：ADMIN / USER */
    private String role;

    /** 自动填充：插入时回填 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 自动填充：插入 + 更新时回填 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;

    /** 是否管理员 */
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(this.role);
    }
}
