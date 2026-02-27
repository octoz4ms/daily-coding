package com.octo.wechatpay.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Native 支付下单请求
 */
@Data
public class NativePrepayRequest {

    /**
     * 商户订单号（唯一）
     */
    @NotBlank(message = "商户订单号不能为空")
    private String outTradeNo;

    /**
     * 商品描述
     */
    @NotBlank(message = "商品描述不能为空")
    private String description;

    /**
     * 支付金额（元），会转换为分
     */
    @NotNull(message = "支付金额不能为空")
    @DecimalMin(value = "0.01", message = "支付金额必须大于0.01元")
    private BigDecimal totalAmount;

    /**
     * 附加数据，回调时原样返回
     */
    private String attach;
}
