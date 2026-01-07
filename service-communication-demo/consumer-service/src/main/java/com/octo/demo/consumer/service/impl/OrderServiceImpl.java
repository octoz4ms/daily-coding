package com.octo.demo.consumer.service.impl;

import com.octo.demo.common.client.UserClient;
import com.octo.demo.common.dto.OrderDTO;
import com.octo.demo.common.dto.Result;
import com.octo.demo.common.dto.UserDTO;
import com.octo.demo.consumer.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务实现类
 * <p>
 * 【标准规范】演示两种服务间调用方式
 * <p>
 * 1. OpenFeign（声明式，推荐）
 *    - 接口定义清晰
 *    - 支持降级、重试
 *    - 自动负载均衡（配合注册中心）
 * <p>
 * 2. RestTemplate（编程式，传统）
 *    - 灵活但代码冗长
 *    - 需要手动处理异常
 *    - 需要手动配置负载均衡
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    /**
     * OpenFeign 客户端（推荐方式）
     */
    private final UserClient userClient;

    /**
     * RestTemplate（传统方式）
     */
    private final RestTemplate restTemplate;

    /**
     * 用户服务地址
     */
    @Value("${provider.service.url}")
    private String providerServiceUrl;

    /**
     * 模拟数据库存储
     */
    private final Map<Long, OrderDTO> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @PostConstruct
    public void init() {
        log.info("初始化订单模拟数据...");
        createOrder(OrderDTO.builder()
                .userId(1L)
                .productName("MacBook Pro")
                .amount(new BigDecimal("12999.00"))
                .status(1)
                .build());
        createOrder(OrderDTO.builder()
                .userId(2L)
                .productName("iPhone 15 Pro")
                .amount(new BigDecimal("8999.00"))
                .status(1)
                .build());
        createOrder(OrderDTO.builder()
                .userId(1L)
                .productName("AirPods Pro")
                .amount(new BigDecimal("1999.00"))
                .status(2)
                .build());
        log.info("订单模拟数据初始化完成，共 {} 条", orderStore.size());
    }

    /**
     * 【方式一】使用 OpenFeign 调用用户服务
     * <p>
     * 优点：
     * - 声明式调用，代码简洁
     * - 支持降级处理
     * - 自动序列化/反序列化
     * - 可配置重试、超时、日志等
     */
    @Override
    public OrderDTO getOrderWithFeign(Long orderId) {
        log.info("【OpenFeign】获取订单，ID: {}", orderId);
        
        OrderDTO order = orderStore.get(orderId);
        if (order == null) {
            return null;
        }

        // 使用 Feign 客户端调用用户服务
        Result<UserDTO> result = userClient.getUserById(order.getUserId());
        
        if (result.isSuccess() && result.getData() != null) {
            order.setUser(result.getData());
            log.info("【OpenFeign】成功获取用户信息: {}", result.getData().getUsername());
        } else {
            log.warn("【OpenFeign】获取用户信息失败: {}", result.getMessage());
        }
        
        return order;
    }

    /**
     * 【方式二】使用 RestTemplate 调用用户服务
     * <p>
     * 优点：
     * - 灵活，可处理复杂场景
     * - 不依赖额外注解
     * <p>
     * 缺点：
     * - 代码冗长
     * - 需要手动处理异常和超时
     * - 需要手动构造请求
     */
    @Override
    public OrderDTO getOrderWithRestTemplate(Long orderId) {
        log.info("【RestTemplate】获取订单，ID: {}", orderId);
        
        OrderDTO order = orderStore.get(orderId);
        if (order == null) {
            return null;
        }

        try {
            // 构造请求URL
            String url = providerServiceUrl + "/api/users/" + order.getUserId();
            
            // 发起HTTP调用
            // 使用 ParameterizedTypeReference 处理泛型
            ResponseEntity<Result<UserDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Result<UserDTO>>() {}
            );

            Result<UserDTO> result = response.getBody();
            if (result != null && result.isSuccess() && result.getData() != null) {
                order.setUser(result.getData());
                log.info("【RestTemplate】成功获取用户信息: {}", result.getData().getUsername());
            }
        } catch (Exception e) {
            log.error("【RestTemplate】调用用户服务失败: {}", e.getMessage());
            // 可以在这里实现降级逻辑
        }
        
        return order;
    }

    @Override
    public List<OrderDTO> listOrders() {
        return new ArrayList<>(orderStore.values());
    }

    @Override
    public OrderDTO createOrder(OrderDTO order) {
        Long id = idGenerator.incrementAndGet();
        order.setId(id);
        order.setOrderNo("ORD" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        order.setCreateTime(LocalDateTime.now());
        orderStore.put(id, order);
        log.info("创建订单成功，ID: {}, 订单号: {}", id, order.getOrderNo());
        return order;
    }
}

