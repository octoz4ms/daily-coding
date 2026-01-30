package com.octo.payment.service;

import com.octo.payment.dto.PaymentRequest;
import com.octo.payment.dto.PaymentResponse;
import com.octo.payment.dto.RefundRequest;

/**
 * 支付服务接口
 */
public interface PaymentService {

    /**
     * 创建支付订单
     */
    PaymentResponse createPayment(PaymentRequest request);

    /**
     * 查询支付订单
     */
    PaymentResponse queryPayment(String orderId);

    /**
     * 关闭支付订单
     */
    boolean closePayment(String orderId);

    /**
     * 申请退款
     */
    boolean refund(RefundRequest request);

    /**
     * 查询退款状态
     */
    PaymentResponse queryRefund(String refundOrderId);
}
