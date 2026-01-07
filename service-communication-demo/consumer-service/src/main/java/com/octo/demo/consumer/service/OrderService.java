package com.octo.demo.consumer.service;

import com.octo.demo.common.dto.OrderDTO;

import java.util.List;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 获取订单（使用 OpenFeign 调用用户服务）
     */
    OrderDTO getOrderWithFeign(Long orderId);

    /**
     * 获取订单（使用 RestTemplate 调用用户服务）
     */
    OrderDTO getOrderWithRestTemplate(Long orderId);

    /**
     * 获取订单列表
     */
    List<OrderDTO> listOrders();

    /**
     * 创建订单
     */
    OrderDTO createOrder(OrderDTO order);
}

