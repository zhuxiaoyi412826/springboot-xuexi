package org.example.mybatisplus.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.mybatisplus.common.BizException;
import org.example.mybatisplus.entity.User;
import org.example.mybatisplus.mapper.UserMapper;
import org.example.mybatisplus.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("账号和密码不能为空");
        }
        User user = this.getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username.trim()));
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (!user.getPassword().equals(password)) {
            throw new BizException("密码错误");
        }
        return user;
    }

    @Override
    public User register(String username, String password, String nickname, String phone, String address) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("账号和密码不能为空");
        }
        Long count = this.lambdaQuery().eq(User::getUsername, username.trim()).count();
        if (count != null && count > 0) {
            throw new BizException("该账号已被注册");
        }
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(password);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : username.trim());
        user.setPhone(phone);
        user.setAddress(address);
        // 注册只能是普通用户
        user.setRole(User.ROLE_USER);
        // createTime / updateTime 由 MyMetaObjectHandler 自动填充
        this.save(user);
        return user;
    }

    @Override
    public User saveUser(User user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new BizException("账号不能为空");
        }
        if (!User.ROLE_ADMIN.equals(user.getRole()) && !User.ROLE_USER.equals(user.getRole())) {
            user.setRole(User.ROLE_USER);
        }
        if (user.getId() == null) {
            // 新增：校验账号重复 + 默认密码
            Long count = this.lambdaQuery().eq(User::getUsername, user.getUsername().trim()).count();
            if (count != null && count > 0) {
                throw new BizException("该账号已存在");
            }
            if (!StringUtils.hasText(user.getPassword())) {
                user.setPassword("123456");
            }
            // createTime 自动填充
        } else {
            // 修改：不允许把账号改成别人的
            User db = this.getById(user.getId());
            if (db == null) {
                throw new BizException("用户不存在");
            }
            if (!db.getUsername().equals(user.getUsername())) {
                Long count = this.lambdaQuery().eq(User::getUsername, user.getUsername().trim()).count();
                if (count != null && count > 0) {
                    throw new BizException("该账号已存在");
                }
            }
            // 前端不传密码表示不修改密码
            if (!StringUtils.hasText(user.getPassword())) {
                user.setPassword(db.getPassword());
            }
        }
        this.saveOrUpdate(user);
        return user;
    }
}
