package com.octo.payment.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * 退款请求DTO
 */
@Data
public class RefundRequest {

    /**
     * 原支付订单号
     */
    @NotBlank(message = "原支付订单号不能为空")
    private String originalOrderId;

    /**
     * 退款订单号
     */
    @NotBlank(message = "退款订单号不能为空")
    private String refundOrderId;

    /**
     * 退款金额
     */
    @NotNull(message = "退款金额不能为空")
    @DecimalMin(value = "0.01", message = "退款金额必须大于0.01元")
    private BigDecimal refundAmount;

    /**
     * 退款原因
     */
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;
}
