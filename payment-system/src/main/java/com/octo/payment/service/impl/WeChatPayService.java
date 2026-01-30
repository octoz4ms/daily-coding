package com.octo.payment.service.impl;

import com.octo.payment.dto.PaymentRequest;
import com.octo.payment.dto.PaymentResponse;
import com.octo.payment.dto.RefundRequest;
import com.octo.payment.entity.PaymentOrder;
import com.octo.payment.enums.PaymentMethod;
import com.octo.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 微信支付服务实现 - 暂时简化实现
 */
@Slf4j
@Service
public class WeChatPayService {

    // TODO: 微信支付SDK依赖暂时注释，需要时取消注释并实现完整功能
    // @Autowired
    // private NativePayService nativePayService;
    //
    // @Value("${wechat.pay.app-id}")
    // private String appId;
    //
    // @Value("${wechat.pay.notify-url}")
    // private String notifyUrl;

    /**
     * 创建微信支付订单 - 暂时返回模拟数据
     */
    public PaymentResponse createPayment(PaymentRequest request, PaymentOrder order) {
        log.info("微信支付创建（模拟），订单号：{}", order.getOrderId());

        // 暂时返回模拟的二维码URL
        return PaymentResponse.builder()
                .orderId(order.getOrderId())
                .merchantOrderId(request.getMerchantOrderId())
                .amount(request.getAmount())
                .paymentMethod(PaymentMethod.WECHAT.getCode())
                .status(PaymentStatus.PENDING.getCode())
                .qrCodeUrl("https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=https://example.com/wechat/pay?orderId=" + order.getOrderId())
                .build();
    }

    /**
     * 微信退款 - 暂时返回成功
     */
    public boolean refund(RefundRequest refundRequest) {
        log.info("微信退款（模拟），订单号：{}，退款金额：{}", refundRequest.getOriginalOrderId(), refundRequest.getRefundAmount());
        return true;
    }

    /**
     * 查询支付状态 - 暂时返回待支付状态
     */
    public PaymentStatus queryPaymentStatus(String orderId) {
        log.info("查询微信支付状态（模拟），订单号：{}", orderId);
        return PaymentStatus.PENDING;
    }
}
