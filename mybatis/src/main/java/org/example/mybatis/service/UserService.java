package org.example.mybatis.service;

import org.example.mybatis.common.PageResult;
import org.example.mybatis.dto.UserQuery;
import org.example.mybatis.entity.User;

import java.util.List;

public interface UserService {

    User login(String username, String password);

    User register(User user);

    User getById(Long id);

    PageResult<User> page(UserQuery query, int pageNum, int pageSize);

    void update(User user);

    void updateStatus(Long id, Integer status);

    void resetPassword(Long id, String newPassword);

    void remove(Long id);

    void batchRemove(List<Long> ids);

    long count();
}
