package com.example.order.dto;

import com.example.common.entity.Order;
import com.example.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String orderNo;
    private Long userId;
    private String productName;
    private Double amount;
    private User user;

    public static OrderDetailResponse from(Order order) {
        OrderDetailResponse resp = new OrderDetailResponse();
        resp.setId(order.getId());
        resp.setOrderNo(order.getOrderNo());
        resp.setUserId(order.getUserId());
        resp.setProductName(order.getProductName());
        resp.setAmount(order.getAmount());
        return resp;
    }
}
