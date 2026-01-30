package com.example.payment.service;

import com.example.payment.dto.request.CreateOrderRequest;
import com.example.payment.dto.response.OrderResponse;
import com.example.payment.entity.Order;

/**
 * 订单服务接口
 */
public interface OrderService {

    /**
     * 生成提交令牌（防重复提交）
     *
     * @param userId 用户ID
     * @return 令牌
     */
    String generateSubmitToken(Long userId);

    /**
     * 创建订单
     *
     * @param request 创建订单请求
     * @return 订单响应
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * 根据订单号查询订单
     *
     * @param orderNo 订单号
     * @return 订单响应
     */
    OrderResponse getOrderByOrderNo(String orderNo);

    /**
     * 根据订单号获取订单实体
     *
     * @param orderNo 订单号
     * @return 订单实体
     */
    Order getEntityByOrderNo(String orderNo);

    /**
     * 取消订单
     *
     * @param orderNo 订单号
     * @param reason  取消原因
     * @return 是否成功
     */
    boolean cancelOrder(String orderNo, String reason);

    /**
     * 支付成功回调处理
     *
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean handlePaySuccess(String orderNo);

    /**
     * 订单超时处理
     *
     * @param orderNo 订单号
     */
    void handleOrderTimeout(String orderNo);
}

