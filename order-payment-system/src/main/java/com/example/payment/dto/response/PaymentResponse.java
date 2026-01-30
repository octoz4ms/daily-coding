package com.example.payment.dto.response;

import com.example.payment.entity.PaymentOrder;
import com.example.payment.enums.PaymentMethod;
import com.example.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    /**
     * 支付单号
     */
    private String paymentNo;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private Integer paymentMethod;

    /**
     * 支付方式描述
     */
    private String paymentMethodDesc;

    /**
     * 支付状态
     */
    private Integer status;

    /**
     * 支付状态描述
     */
    private String statusDesc;

    /**
     * 支付二维码/链接
     */
    private String payUrl;

    /**
     * 预支付ID（微信）
     */
    private String prepayId;

    /**
     * 第三方交易号
     */
    private String transactionId;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 支付时间
     */
    private LocalDateTime payTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 微信支付参数（JSAPI/小程序支付使用）
     */
    private WechatPayParams wechatPayParams;

    /**
     * 支付宝Form表单（H5支付使用）
     */
    private String alipayForm;

    /**
     * 从实体转换
     */
    public static PaymentResponse fromEntity(PaymentOrder paymentOrder) {
        if (paymentOrder == null) {
            return null;
        }

        PaymentMethod method = PaymentMethod.fromCode(paymentOrder.getPaymentMethod());
        PaymentStatus status = PaymentStatus.fromCode(paymentOrder.getStatus());

        return PaymentResponse.builder()
                .paymentNo(paymentOrder.getPaymentNo())
                .orderNo(paymentOrder.getOrderNo())
                .amount(paymentOrder.getAmount())
                .paymentMethod(paymentOrder.getPaymentMethod())
                .paymentMethodDesc(method != null ? method.getDesc() : "未知")
                .status(paymentOrder.getStatus())
                .statusDesc(status != null ? status.getDesc() : "未知")
                .payUrl(paymentOrder.getPayUrl())
                .prepayId(paymentOrder.getPrepayId())
                .transactionId(paymentOrder.getTransactionId())
                .expireTime(paymentOrder.getExpireTime())
                .payTime(paymentOrder.getPayTime())
                .createTime(paymentOrder.getCreateTime())
                .build();
    }

    /**
     * 微信支付参数
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WechatPayParams {
        private String appId;
        private String timeStamp;
        private String nonceStr;
        private String packageValue;
        private String signType;
        private String paySign;
    }
}

