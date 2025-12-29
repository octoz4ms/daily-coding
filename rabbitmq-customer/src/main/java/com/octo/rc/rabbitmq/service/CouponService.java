package com.octo.rc.rabbitmq.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 优惠券服务
 * 负责优惠券的使用和释放
 */
@Service
@Slf4j
public class CouponService {

    /**
     * 模拟优惠券存储
     * key: 优惠券ID, value: 优惠券信息
     */
    private final Map<String, Coupon> couponStorage = new ConcurrentHashMap<>();

    /**
     * 模拟订单使用的优惠券映射
     * key: 订单ID, value: 优惠券ID
     */
    private final Map<String, String> orderCouponMapping = new ConcurrentHashMap<>();

    /**
     * 初始化优惠券（模拟数据）
     */
    public CouponService() {
        // 初始化一些优惠券
        couponStorage.put("c001", new Coupon("c001", "新人满100减20", new BigDecimal("20"), CouponStatus.AVAILABLE));
        couponStorage.put("c002", new Coupon("c002", "会员满500减50", new BigDecimal("50"), CouponStatus.AVAILABLE));
        couponStorage.put("c003", new Coupon("c003", "双12满1000减100", new BigDecimal("100"), CouponStatus.AVAILABLE));
    }

    /**
     * 使用优惠券
     *
     * @param orderId  订单ID
     * @param couponId 优惠券ID
     * @return 优惠金额，如果使用失败返回 0
     */
    public BigDecimal useCoupon(String orderId, String couponId) {
        if (couponId == null || couponId.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponStorage.get(couponId);
        if (coupon == null) {
            log.warn("优惠券不存在，优惠券ID: {}", couponId);
            return BigDecimal.ZERO;
        }

        if (coupon.getStatus() != CouponStatus.AVAILABLE) {
            log.warn("优惠券不可用，优惠券ID: {}, 当前状态: {}", couponId, coupon.getStatus());
            return BigDecimal.ZERO;
        }

        // 标记优惠券为已使用
        coupon.setStatus(CouponStatus.USED);
        // 记录订单与优惠券的映射
        orderCouponMapping.put(orderId, couponId);

        log.info("优惠券使用成功，订单ID: {}, 优惠券ID: {}, 优惠券名称: {}, 优惠金额: {}",
                orderId, couponId, coupon.getName(), coupon.getAmount());
        return coupon.getAmount();
    }

    /**
     * 释放优惠券
     * 订单取消时释放已使用的优惠券
     *
     * @param orderId 订单ID
     * @return 是否释放成功
     */
    public boolean releaseCoupon(String orderId) {
        String couponId = orderCouponMapping.get(orderId);
        if (couponId == null) {
            log.debug("订单未使用优惠券，订单ID: {}", orderId);
            return false;
        }

        Coupon coupon = couponStorage.get(couponId);
        if (coupon != null) {
            coupon.setStatus(CouponStatus.AVAILABLE);
            orderCouponMapping.remove(orderId);

            log.info("优惠券释放成功，订单ID: {}, 优惠券ID: {}, 优惠券名称: {}",
                    orderId, couponId, coupon.getName());
            return true;
        }

        return false;
    }

    /**
     * 确认使用优惠券
     * 订单支付成功后确认优惠券使用
     *
     * @param orderId 订单ID
     */
    public void confirmCoupon(String orderId) {
        String couponId = orderCouponMapping.get(orderId);
        if (couponId != null) {
            Coupon coupon = couponStorage.get(couponId);
            if (coupon != null) {
                coupon.setStatus(CouponStatus.CONSUMED);
                log.info("优惠券使用确认，订单ID: {}, 优惠券ID: {}", orderId, couponId);
            }
        }
    }

    /**
     * 获取优惠券信息
     *
     * @param couponId 优惠券ID
     * @return 优惠券信息
     */
    public Coupon getCoupon(String couponId) {
        return couponStorage.get(couponId);
    }

    /**
     * 优惠券实体类
     */
    @Data
    public static class Coupon {
        private String couponId;
        private String name;
        private BigDecimal amount;
        private CouponStatus status;

        public Coupon(String couponId, String name, BigDecimal amount, CouponStatus status) {
            this.couponId = couponId;
            this.name = name;
            this.amount = amount;
            this.status = status;
        }
    }

    /**
     * 优惠券状态枚举
     */
    public enum CouponStatus {
        /**
         * 可用
         */
        AVAILABLE,
        /**
         * 已使用（待确认）
         */
        USED,
        /**
         * 已消费（已确认）
         */
        CONSUMED,
        /**
         * 已过期
         */
        EXPIRED
    }
}

