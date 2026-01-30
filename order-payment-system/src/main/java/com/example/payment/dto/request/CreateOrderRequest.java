package com.example.payment.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.*;
import java.math.BigDecimal;

/**
 * 创建订单请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 商品ID
     */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /**
     * 购买数量
     */
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为1")
    @Max(value = 99, message = "购买数量不能超过99")
    private Integer quantity;

    /**
     * 优惠券ID（可选）
     */
    private Long couponId;

    /**
     * 使用积分（可选）
     */
    private Integer usePoints;

    /**
     * 收货人姓名
     */
    @NotBlank(message = "收货人姓名不能为空")
    @Size(max = 50, message = "收货人姓名不能超过50字符")
    private String receiverName;

    /**
     * 收货人电话
     */
    @NotBlank(message = "收货人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String receiverPhone;

    /**
     * 收货地址
     */
    @NotBlank(message = "收货地址不能为空")
    @Size(max = 200, message = "收货地址不能超过200字符")
    private String receiverAddress;

    /**
     * 订单备注
     */
    @Size(max = 500, message = "备注不能超过500字符")
    private String remark;

    /**
     * 提交令牌（防重复提交）
     */
    @NotBlank(message = "提交令牌不能为空")
    private String submitToken;
}

