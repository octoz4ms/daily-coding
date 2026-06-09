package com.example.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "用户ID不能为空")
    @Positive(message = "用户ID必须为正数")
    private Long userId;

    @NotBlank(message = "商品名称不能为空")
    private String productName;

    @NotNull(message = "订单金额不能为空")
    @Positive(message = "订单金额必须大于0")
    private Double amount;
}
