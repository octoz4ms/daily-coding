package com.octo.demo.provider.service;

import com.octo.demo.common.dto.UserDTO;

import java.util.List;

/**
 * 用户服务接口
 */
public interface UserService {

    UserDTO getUserById(Long id);

    List<UserDTO> listUsers();

    List<UserDTO> getUsersByIds(List<Long> ids);

    UserDTO createUser(UserDTO user);

    UserDTO updateUser(UserDTO user);

    void deleteUser(Long id);
}

