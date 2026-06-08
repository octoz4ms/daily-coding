package com.example.order.dto;

import com.example.common.entity.Order;
import com.example.common.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderDetailResponse {

    private Order order;
    private User user;
    private boolean degraded;
}
