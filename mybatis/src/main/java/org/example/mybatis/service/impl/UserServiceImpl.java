package org.example.mybatis.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.mybatis.common.BizException;
import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.UserQuery;
import org.example.mybatis.entity.User;
import org.example.mybatis.mapper.UserMapper;
import org.example.mybatis.plugin.PageContext;
import org.example.mybatis.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public User login(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new BizException("账号和密码不能为空");
        }
        User user = userMapper.selectByUsername(username.trim());
        if (user == null) {
            throw new BizException("账号不存在");
        }
        if (!password.equals(user.getPassword())) {
            throw new BizException("密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("账号已被禁用，请联系管理员");
        }
        // 不把密码返回给前端
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public User register(User user) {
        if (!StringUtils.hasText(user.getUsername()) || !StringUtils.hasText(user.getPassword())) {
            throw new BizException("账号和密码不能为空");
        }
        if (userMapper.selectByUsername(user.getUsername().trim()) != null) {
            throw new BizException("该账号已被注册");
        }
        user.setUsername(user.getUsername().trim());
        if (!StringUtils.hasText(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        // 注册只能是普通用户，避免有人注册管理员
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    /**
     * 分页查询 —— 这里演示自研分页插件的用法：
     * PageContext.start() 之后紧跟一次查询，拦截器会自动追加 LIMIT 并回填 total。
     */
    @Override
    public PageResult<User> page(UserQuery query, int pageNum, int pageSize) {
        try {
            PageContext.start(pageNum, pageSize);
            List<User> list = userMapper.selectByCondition(query == null ? new UserQuery() : query);
            long total = PageContext.getTotal();
            // 抹掉密码
            list.forEach(u -> u.setPassword(null));
            return PageResult.of(list, total, pageNum, pageSize);
        } finally {
            PageContext.clear();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(User user) {
        User db = userMapper.selectById(user.getId());
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if ("ADMIN".equals(db.getRole()) && user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("不能禁用管理员账号");
        }
        userMapper.update(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        User db = userMapper.selectById(id);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if ("ADMIN".equals(db.getRole()) && status != null && status == 0) {
            throw new BizException("不能禁用管理员账号");
        }
        userMapper.updateStatus(id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(Long id, String newPassword) {
        if (!StringUtils.hasText(newPassword) || newPassword.length() < 6) {
            throw new BizException("密码至少 6 位");
        }
        userMapper.updatePassword(id, newPassword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        User db = userMapper.selectById(id);
        if (db == null) {
            throw new BizException("用户不存在");
        }
        if (db.isAdmin()) {
            throw new BizException("不能删除管理员账号");
        }
        userMapper.logicDelete(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchRemove(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的用户");
        }
        // 批量操作走 XML 的 foreach
        userMapper.batchLogicDelete(ids);
    }

    @Override
    public long count() {
        return userMapper.count();
    }
}
