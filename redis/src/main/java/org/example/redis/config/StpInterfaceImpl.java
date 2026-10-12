package org.example.redis.config;

import cn.dev33.satoken.stp.StpInterface;
import lombok.RequiredArgsConstructor;
import org.example.redis.service.UserService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sa-Token 的权限数据源：告诉框架「这个人有什么角色、什么权限」。
 *
 * <p>知识点：角色/权限判断在每次鉴权都会执行，所以这里走三级缓存，
 * 不要每次都查库（否则 @SaCheckRole 越用越慢）。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final UserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return userService.permissionOf(Long.parseLong(loginId.toString()));
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of(userService.roleOf(Long.parseLong(loginId.toString())));
    }
}
