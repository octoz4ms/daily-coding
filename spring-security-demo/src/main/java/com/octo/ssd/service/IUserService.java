package com.octo.ssd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.octo.ssd.dto.UserDTO;
import com.octo.ssd.entity.User;

/**
 * 用户服务接口
 *
 * @author octo
 */
public interface IUserService extends IService<User> {

    /**
     * 分页查询用户列表
     *
     * @param page     当前页
     * @param size     每页大小
     * @param username 用户名（模糊查询）
     * @return 用户分页列表
     */
    IPage<User> getUserPage(int page, int size, String username);

    /**
     * 根据ID获取用户详情（包含角色信息）
     *
     * @param userId 用户ID
     * @return 用户详情
     */
    User getUserDetail(Long userId);

    /**
     * 创建用户
     *
     * @param userDTO 用户信息
     * @return 创建的用户
     */
    User createUser(UserDTO userDTO);

    /**
     * 更新用户
     *
     * @param userDTO 用户信息
     * @return 更新后的用户
     */
    User updateUser(UserDTO userDTO);

    /**
     * 删除用户
     *
     * @param userId 用户ID
     */
    void deleteUser(Long userId);

    /**
     * 为用户分配角色
     *
     * @param userId  用户ID
     * @param roleIds 角色ID列表
     */
    void assignRoles(Long userId, java.util.List<Long> roleIds);

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getByUsername(String username);
}
