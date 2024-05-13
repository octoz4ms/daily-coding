package com.octo.ssd.service.impl;

import com.octo.ssd.entity.User;
import com.octo.ssd.mapper.UserMapper;
import com.octo.ssd.service.IUserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author zms
 * @since 2024-04-25
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

}
