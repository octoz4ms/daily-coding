package com.octo.eum.security;

import com.octo.eum.common.ResultCode;
import com.octo.eum.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 安全工具类
 *
 * @author octo
 */
public class SecurityUtils {

    private SecurityUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取当前登录用户
     */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户（必须登录）
     */
    public static LoginUser getRequiredCurrentUser() {
        LoginUser user = getCurrentUser();
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return user;
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getUserId() : null;
    }

    /**
     * 获取当前用户ID（必须登录）
     */
    public static Long getRequiredCurrentUserId() {
        return getRequiredCurrentUser().getUserId();
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUsername() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前用户角色列表
     */
    public static List<String> getCurrentUserRoles() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getRoles() : List.of();
    }

    /**
     * 获取当前用户权限列表
     */
    public static List<String> getCurrentUserPermissions() {
        LoginUser user = getCurrentUser();
        return user != null ? user.getPermissions() : List.of();
    }

    /**
     * 判断当前用户是否有指定角色
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * 判断当前用户是否有指定权限
     */
    public static boolean hasPermission(String permission) {
        return getCurrentUserPermissions().contains(permission);
    }

    /**
     * 判断当前用户是否是管理员
     */
    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }

    /**
     * 判断是否已登录
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof LoginUser;
    }
}

