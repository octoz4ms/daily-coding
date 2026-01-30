package com.octo.payment.service.impl;

import com.octo.payment.dto.PaymentRequest;
import com.octo.payment.dto.PaymentResponse;
import com.octo.payment.dto.RefundRequest;
import com.octo.payment.entity.PaymentOrder;
import com.octo.payment.enums.PaymentMethod;
import com.octo.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 支付宝支付服务实现 - 暂时简化实现
 */
@Slf4j
@Service
public class AlipayService {

    // TODO: 支付宝SDK依赖暂时注释，需要时取消注释并实现完整功能
    // @Autowired
    // private AlipayClient alipayClient;
    //
    // @Value("${alipay.notify-url}")
    // private String notifyUrl;
    //
    // @Value("${alipay.return-url}")
    // private String returnUrl;

    /**
     * 创建支付宝支付订单 - 暂时返回模拟数据
     */
    public PaymentResponse createPayment(PaymentRequest request, PaymentOrder order) {
        log.info("支付宝支付创建（模拟），订单号：{}", order.getOrderId());

        // 暂时返回模拟的支付URL
        return PaymentResponse.builder()
                .orderId(order.getOrderId())
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.ALIPAY.getCode())
                .status(PaymentStatus.PENDING.getCode())
                .payUrl("https://example.com/alipay/pay?orderId=" + order.getOrderId())
                .build();
    }

    /**
     * 支付宝退款 - 暂时返回成功
     */
    public boolean refund(RefundRequest refundRequest) {
        log.info("支付宝退款（模拟），订单号：{}，退款金额：{}", refundRequest.getOriginalOrderId(), refundRequest.getRefundAmount());
        return true;
    }

    /**
     * 查询支付状态 - 暂时返回待支付状态
     */
    public PaymentStatus queryPaymentStatus(String orderId) {
        log.info("查询支付宝支付状态（模拟），订单号：{}", orderId);
        return PaymentStatus.PENDING;
    }
}
