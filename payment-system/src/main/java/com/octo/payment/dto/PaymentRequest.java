package com.octo.payment.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 支付请求DTO
 */
@Data
public class PaymentRequest {

    /**
     * 商户订单号
     */
    @NotBlank(message = "商户订单号不能为空")
    private String merchantOrderId;

    /**
     * 用户ID
     */
    @NotBlank(message = "用户ID不能为空")
    private String userId;

    /**
     * 支付金额
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0.01元")
    private BigDecimal amount;

    /**
     * 支付方式
     */
    @NotBlank(message = "支付方式不能为空")
    private String paymentMethod;

    /**
     * 商品描述
     */
    @NotBlank(message = "商品描述不能为空")
    private String description;

    /**
     * 回调通知URL
     */
    private String notifyUrl;

    /**
     * 支付成功跳转URL
     */
    private String returnUrl;

    /**
     * 附加数据
     */
    private String attach;
}
