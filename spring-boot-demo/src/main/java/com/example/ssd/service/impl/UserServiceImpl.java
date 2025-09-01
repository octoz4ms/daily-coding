package com.example.ssd.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.ssd.annotation.Log;
import com.example.ssd.entity.User;
import com.example.ssd.mapper.UserMapper;
import com.example.ssd.service.IUserService;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {


    @Override
    @Log("获取用户名")
    public String getUserName(Long userId) {
        return "张名帅";
    }
}
