package com.example.order.service;

import com.example.common.entity.Order;
import com.example.common.entity.User;
import com.example.common.result.Result;
import com.example.feign.client.UserFeignClient;
import com.example.order.dto.OrderCreateRequest;
import com.example.order.dto.OrderDetailResponse;
import com.example.order.exception.BusinessException;
import com.example.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserFeignClient userFeignClient;

    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("订单不存在"));

        OrderDetailResponse response = OrderDetailResponse.from(order);

        User user = fetchUser(order.getUserId());
        if (user == null) {
            throw new BusinessException("无法获取用户信息，订单详情暂不可用");
        }
        response.setUser(user);

        return response;
    }

    public OrderDetailResponse createOrder(OrderCreateRequest request) {
        User user = fetchUser(request.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在，下单失败");
        }

        Order order = new Order();
        order.setUserId(request.getUserId());
        order.setProductName(request.getProductName());
        order.setAmount(request.getAmount());
        order.setOrderNo("ORD" + System.currentTimeMillis());

        Order saved = orderRepository.save(order);

        OrderDetailResponse response = OrderDetailResponse.from(saved);
        response.setUser(user);
        return response;
    }

    private User fetchUser(Long userId) {
        Result<User> result = userFeignClient.getUserById(userId);
        if (result == null) {
            log.warn("[OrderService] 用户服务响应为空, userId={}", userId);
            return null;
        }
        if (result.getCode() == null || result.getCode() != 200) {
            log.warn("[OrderService] 用户服务返回业务错误, code={}, message={}, userId={}",
                    result.getCode(), result.getMessage(), userId);
            return null;
        }
        return result.getData();
    }
}
