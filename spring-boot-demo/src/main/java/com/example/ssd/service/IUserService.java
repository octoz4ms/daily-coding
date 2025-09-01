package com.example.ssd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.ssd.entity.User;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
public interface IUserService extends IService<User> {

    public String getUserName(Long userId);
}
