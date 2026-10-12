package org.example.redis.vo;

import lombok.Data;
import org.example.redis.entity.User;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 用户视图对象（不返回密码）。 */
@Data
public class UserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private Integer status;
    private Integer signDays;
    private LocalDateTime createTime;

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setSignDays(user.getSignDays());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
