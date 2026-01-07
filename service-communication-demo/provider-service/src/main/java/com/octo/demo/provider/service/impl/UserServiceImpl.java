package com.octo.demo.provider.service.impl;

import com.octo.demo.common.dto.UserDTO;
import com.octo.demo.provider.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务实现类
 * <p>
 * 使用内存存储模拟数据库操作
 */
@Slf4j
@Service
public class UserServiceImpl implements UserService {

    /**
     * 模拟数据库存储
     */
    private final Map<Long, UserDTO> userStore = new ConcurrentHashMap<>();

    private final AtomicLong idGenerator = new AtomicLong(0);

    /**
     * 初始化模拟数据
     */
    @PostConstruct
    public void init() {
        log.info("初始化用户模拟数据...");
        createUser(UserDTO.builder()
                .username("张三")
                .email("zhangsan@example.com")
                .phone("13800138001")
                .status(1)
                .build());
        createUser(UserDTO.builder()
                .username("李四")
                .email("lisi@example.com")
                .phone("13800138002")
                .status(1)
                .build());
        createUser(UserDTO.builder()
                .username("王五")
                .email("wangwu@example.com")
                .phone("13800138003")
                .status(1)
                .build());
        log.info("用户模拟数据初始化完成，共 {} 条", userStore.size());
    }

    @Override
    public UserDTO getUserById(Long id) {
        log.debug("查询用户，ID: {}", id);
        return userStore.get(id);
    }

    @Override
    public List<UserDTO> listUsers() {
        log.debug("查询所有用户");
        return new ArrayList<>(userStore.values());
    }

    @Override
    public List<UserDTO> getUsersByIds(List<Long> ids) {
        log.debug("批量查询用户，IDs: {}", ids);
        List<UserDTO> result = new ArrayList<>();
        for (Long id : ids) {
            UserDTO user = userStore.get(id);
            if (user != null) {
                result.add(user);
            }
        }
        return result;
    }

    @Override
    public UserDTO createUser(UserDTO user) {
        Long id = idGenerator.incrementAndGet();
        user.setId(id);
        user.setCreateTime(LocalDateTime.now());
        userStore.put(id, user);
        log.info("创建用户成功，ID: {}", id);
        return user;
    }

    @Override
    public UserDTO updateUser(UserDTO user) {
        if (!userStore.containsKey(user.getId())) {
            throw new RuntimeException("用户不存在: " + user.getId());
        }
        UserDTO existing = userStore.get(user.getId());
        // 保留创建时间
        user.setCreateTime(existing.getCreateTime());
        userStore.put(user.getId(), user);
        log.info("更新用户成功，ID: {}", user.getId());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        if (!userStore.containsKey(id)) {
            throw new RuntimeException("用户不存在: " + id);
        }
        userStore.remove(id);
        log.info("删除用户成功，ID: {}", id);
    }
}

