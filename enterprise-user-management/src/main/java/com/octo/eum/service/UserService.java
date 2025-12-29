package com.octo.eum.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.eum.common.PageResult;
import com.octo.eum.dto.request.PasswordUpdateRequest;
import com.octo.eum.dto.request.UserCreateRequest;
import com.octo.eum.dto.request.UserQueryRequest;
import com.octo.eum.dto.request.UserUpdateRequest;
import com.octo.eum.dto.response.UserVO;
import com.octo.eum.entity.User;

import java.util.List;

/**
 * 用户服务接口
 *
 * @author octo
 */
public interface UserService extends IService<User> {

    /**
     * 创建用户
     *
     * @param request 创建请求
     * @return 用户ID
     */
    Long createUser(UserCreateRequest request);

    /**
     * 更新用户
     *
     * @param request 更新请求
     */
    void updateUser(UserUpdateRequest request);

    /**
     * 删除用户
     *
     * @param id 用户ID
     */
    void deleteUser(Long id);

    /**
     * 批量删除用户
     *
     * @param ids 用户ID列表
     */
    void batchDeleteUsers(List<Long> ids);

    /**
     * 根据ID获取用户
     *
     * @param id 用户ID
     * @return 用户VO
     */
    UserVO getUserById(Long id);

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户VO
     */
    UserVO getUserByUsername(String username);

    /**
     * 分页查询用户
     *
     * @param request 查询请求
     * @return 分页结果
     */
    PageResult<UserVO> pageUsers(UserQueryRequest request);

    /**
     * 修改密码
     *
     * @param request 密码修改请求
     */
    void updatePassword(PasswordUpdateRequest request);

    /**
     * 重置密码
     *
     * @param userId      用户ID
     * @param newPassword 新密码
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 更新用户状态
     *
     * @param userId 用户ID
     * @param status 状态
     */
    void updateStatus(Long userId, Integer status);

    /**
     * 分配角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 检查用户名是否存在
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 更新登录信息
     *
     * @param userId 用户ID
     * @param ip     登录IP
     */
    void updateLoginInfo(Long userId, String ip);
}

