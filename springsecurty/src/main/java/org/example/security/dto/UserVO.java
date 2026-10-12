package org.example.security.dto;

import lombok.Data;
import org.example.security.entity.User;

import java.time.LocalDateTime;
import java.util.Set;

/** 用户 VO：对外不暴露 password / digestHash */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private Boolean enabled;
    private Boolean accountNonLocked;
    private String source;
    private Set<String> roles;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private LocalDateTime createTime;

    public static UserVO from(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setEmail(u.getEmail());
        vo.setPhone(u.getPhone());
        vo.setAvatar(u.getAvatar());
        vo.setEnabled(u.getEnabled());
        vo.setAccountNonLocked(u.getAccountNonLocked());
        vo.setSource(u.getSource());
        vo.setRoles(u.getRoles());
        vo.setLastLoginTime(u.getLastLoginTime());
        vo.setLastLoginIp(u.getLastLoginIp());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }
}
