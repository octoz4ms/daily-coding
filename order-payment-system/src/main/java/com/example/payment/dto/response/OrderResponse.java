package com.example.payment.dto.response;

import com.example.payment.entity.Order;
import com.example.payment.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品数量
     */
    private Integer quantity;

    /**
     * 商品单价
     */
    private BigDecimal unitPrice;

    /**
     * 商品总价
     */
    private BigDecimal totalAmount;

    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;

    /**
     * 运费
     */
    private BigDecimal freightAmount;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态码
     */
    private Integer status;

    /**
     * 订单状态描述
     */
    private String statusDesc;

    /**
     * 收货人姓名
     */
    private String receiverName;

    /**
     * 收货人电话
     */
    private String receiverPhone;

    /**
     * 收货地址
     */
    private String receiverAddress;

    /**
     * 订单备注
     */
    private String remark;

    /**
     * 订单过期时间
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
     * 剩余支付时间（秒）
     */
    private Long remainingPayTime;

    /**
     * 从实体转换
     */
    public static OrderResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }

        OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
        
        OrderResponse response = OrderResponse.builder()
                .orderNo(order.getOrderNo())
                .userId(order.getUserId())
                .productId(order.getProductId())
                .productName(order.getProductName())
                .quantity(order.getQuantity())
                .unitPrice(order.getUnitPrice())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .freightAmount(order.getFreightAmount())
                .payAmount(order.getPayAmount())
                .status(order.getStatus())
                .statusDesc(orderStatus != null ? orderStatus.getDesc() : "未知")
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .remark(order.getRemark())
                .expireTime(order.getExpireTime())
                .payTime(order.getPayTime())
                .createTime(order.getCreateTime())
                .build();

        // 计算剩余支付时间
        if (order.getExpireTime() != null && orderStatus == OrderStatus.PENDING_PAYMENT) {
            long remaining = java.time.Duration.between(LocalDateTime.now(), order.getExpireTime()).getSeconds();
            response.setRemainingPayTime(Math.max(0, remaining));
        }

        return response;
    }
}

