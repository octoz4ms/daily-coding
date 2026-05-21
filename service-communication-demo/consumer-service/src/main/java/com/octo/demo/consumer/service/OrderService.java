package com.octo.demo.consumer.service;

import com.octo.demo.common.dto.OrderDTO;

import java.util.List;
import java.util.Map;

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
     * 获取订单（使用 WebClient 调用用户服务）
     */
    OrderDTO getOrderWithWebClient(Long orderId);

    /**
     * 对比几种服务调用方式
     */
    Map<String, Object> compareCommunicationMethods();

    /**
     * 对比本地部署与跨服务器部署的差异
     */
    Map<String, Object> compareDeploymentScenarios();

    /**
     * 获取订单列表
     */
    List<OrderDTO> listOrders();

    /**
     * 创建订单
     */
    OrderDTO createOrder(OrderDTO order);

    OrderDTO getOrderWithRestTemplateInPost(Long id);
}

