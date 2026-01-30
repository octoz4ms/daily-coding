package com.example.payment.service;

import com.example.payment.dto.request.PayOrderRequest;
import com.example.payment.dto.response.PaymentResponse;

import java.util.Map;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付单
     *
     * @param request 支付请求
     * @return 支付响应
     */
    PaymentResponse createPayment(PayOrderRequest request);

    /**
     * 查询支付单
     *
     * @param paymentNo 支付单号
     * @return 支付响应
     */
    PaymentResponse queryPayment(String paymentNo);

    /**
     * 根据订单号查询支付单
     *
     * @param orderNo 订单号
     * @return 支付响应
     */
    PaymentResponse queryPaymentByOrderNo(String orderNo);

    /**
     * 关闭支付单
     *
     * @param paymentNo 支付单号
     * @return 是否成功
     */
    boolean closePayment(String paymentNo);

    /**
     * 处理微信支付回调
     *
     * @param params 回调参数
     * @return 是否处理成功
     */
    boolean handleWechatCallback(Map<String, String> params);

    /**
     * 处理支付宝回调
     *
     * @param params 回调参数
     * @return 是否处理成功
     */
    boolean handleAlipayCallback(Map<String, String> params);

    /**
     * 主动查询第三方支付状态
     *
     * @param paymentNo 支付单号
     * @return 支付响应
     */
    PaymentResponse syncPaymentStatus(String paymentNo);
}

