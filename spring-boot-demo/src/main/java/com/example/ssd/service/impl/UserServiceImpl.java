package com.example.ssd.service.impl;

import com.example.ssd.entity.User;
import com.example.ssd.mapper.UserMapper;
import com.example.ssd.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zms
 * @since 2024-05-30
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
