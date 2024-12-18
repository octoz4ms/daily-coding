package com.octo.cloud.us.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.octo.cloud.us.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Override
    @SentinelResource(value = "chain")
    public void charinTest() {

    }
}
