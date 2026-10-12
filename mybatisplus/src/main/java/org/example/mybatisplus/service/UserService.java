package org.example.mybatisplus.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.example.mybatisplus.entity.User;

public interface UserService extends IService<User> {

    /**
     * 登录校验，成功返回用户信息
     */
    User login(String username, String password);

    /**
     * 注册（固定为普通用户，避免随便注册管理员）
     */
    User register(String username, String password, String nickname, String phone, String address);

    /**
     * 新增 / 修改用户（后台管理用，可指定角色）
     */
    User saveUser(User user);
}
