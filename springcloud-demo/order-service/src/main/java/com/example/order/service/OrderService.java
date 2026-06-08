package com.example.order.service;

import com.example.common.entity.Order;
import com.example.common.entity.User;
import com.example.order.dto.OrderDetailResponse;
import com.example.order.repository.OrderRepository;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OrderService {

    @Resource
    private OrderRepository orderRepository;

    @Resource
    private UserRemoteService userRemoteService;

    public Order getOrderById(Long id) {
        return orderRepository.findById(id);
    }

    public Map<Long, Order> listOrders() {
        return orderRepository.findAll();
    }

    public OrderDetailResponse getOrderWithUserStrict(Long id) {
        Order order = requireOrder(id);
        User user = userRemoteService.getUserStrict(order.getUserId());
        return new OrderDetailResponse(order, user, false);
    }

    public OrderDetailResponse getOrderWithUserTolerant(Long id) {
        Order order = requireOrder(id);
        User user = userRemoteService.getUserWithDefault(order.getUserId());
        return new OrderDetailResponse(order, user, userRemoteService.isDefaultUser(user));
    }

    private Order requireOrder(Long id) {
        Order order = orderRepository.findById(id);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        return order;
    }
}
