package com.octo.demo.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long userId;

    private String productName;

    private BigDecimal amount;

    private Integer status;

    private LocalDateTime createTime;

    /**
     * 关联的用户信息（跨服务获取）
     */
    private UserDTO user;
}

