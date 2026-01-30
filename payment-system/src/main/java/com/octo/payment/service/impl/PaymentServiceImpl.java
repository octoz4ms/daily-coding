package com.octo.payment.service.impl;

import com.octo.payment.dto.PaymentRequest;
import com.octo.payment.dto.PaymentResponse;
import com.octo.payment.dto.RefundRequest;
import com.octo.payment.entity.PaymentOrder;
import com.octo.payment.enums.PaymentMethod;
import com.octo.payment.enums.PaymentStatus;
import com.octo.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private WeChatPayService weChatPayService;

    @Autowired
    private AlipayService alipayService;

    private static final String PAYMENT_ORDER_KEY_PREFIX = "payment:order:";

    @Override
    public PaymentResponse createPayment(PaymentRequest request) {
        try {
            // 生成订单号
            String orderId = generateOrderId();

            // 创建支付订单
            PaymentOrder order = PaymentOrder.builder()
                    .orderId(orderId)
                    .merchantOrderId(request.getMerchantOrderId())
                    .userId(request.getUserId())
                    .amount(request.getAmount())
                    .paymentMethod(PaymentMethod.fromCode(request.getPaymentMethod()))
                    .status(PaymentStatus.PENDING)
                    .description(request.getDescription())
                    .createTime(LocalDateTime.now())
                    .expireTime(LocalDateTime.now().plusMinutes(30)) // 30分钟过期
                    .notifyUrl(request.getNotifyUrl())
                    .returnUrl(request.getReturnUrl())
                    .attach(request.getAttach())
                    .build();

            // 保存订单到Redis（生产环境建议使用数据库）
            String key = PAYMENT_ORDER_KEY_PREFIX + orderId;
            redisTemplate.opsForValue().set(key, order, 30, TimeUnit.MINUTES);

            log.info("创建支付订单成功，订单号：{}，支付方式：{}", orderId, request.getPaymentMethod());

            // 根据支付方式调用对应的支付服务
            PaymentResponse response;
            if (PaymentMethod.WECHAT.getCode().equals(request.getPaymentMethod())) {
                response = weChatPayService.createPayment(request, order);
            } else if (PaymentMethod.ALIPAY.getCode().equals(request.getPaymentMethod())) {
                response = alipayService.createPayment(request, order);
            } else {
                return PaymentResponse.builder()
                        .orderId(orderId)
                        .merchantOrderId(request.getMerchantOrderId())
                        .amount(request.getAmount())
                        .paymentMethod(request.getPaymentMethod())
                        .status(PaymentStatus.FAILED.getCode())
                        .errorMessage("不支持的支付方式")
                        .build();
            }

            return response;

        } catch (Exception e) {
            log.error("创建支付订单失败：{}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .merchantOrderId(request.getMerchantOrderId())
                    .amount(request.getAmount())
                    .paymentMethod(request.getPaymentMethod())
                    .status(PaymentStatus.FAILED.getCode())
                    .errorMessage("创建支付订单失败：" + e.getMessage())
                    .build();
        }
    }

    @Override
    public PaymentResponse queryPayment(String orderId) {
        try {
            String key = PAYMENT_ORDER_KEY_PREFIX + orderId;
            PaymentOrder order = (PaymentOrder) redisTemplate.opsForValue().get(key);

            if (order == null) {
                return PaymentResponse.builder()
                        .orderId(orderId)
                        .status(PaymentStatus.FAILED.getCode())
                        .errorMessage("订单不存在")
                        .build();
            }

            // 查询支付平台状态
            PaymentStatus platformStatus;
            if (PaymentMethod.WECHAT.equals(order.getPaymentMethod())) {
                platformStatus = weChatPayService.queryPaymentStatus(orderId);
            } else if (PaymentMethod.ALIPAY.equals(order.getPaymentMethod())) {
                platformStatus = alipayService.queryPaymentStatus(orderId);
            } else {
                platformStatus = order.getStatus();
            }

            // 更新订单状态
            if (!platformStatus.equals(order.getStatus())) {
                order.setStatus(platformStatus);
                if (PaymentStatus.PAID.equals(platformStatus)) {
                    order.setPaidTime(LocalDateTime.now());
                }
                redisTemplate.opsForValue().set(key, order, 30, TimeUnit.MINUTES);
            }

            return PaymentResponse.builder()
                    .orderId(order.getOrderId())
                    .merchantOrderId(order.getMerchantOrderId())
                    .amount(order.getAmount())
                    .paymentMethod(order.getPaymentMethod().getCode())
                    .status(order.getStatus().getCode())
                    .build();

        } catch (Exception e) {
            log.error("查询支付订单失败：{}", e.getMessage(), e);
            return PaymentResponse.builder()
                    .orderId(orderId)
                    .status(PaymentStatus.FAILED.getCode())
                    .errorMessage("查询失败：" + e.getMessage())
                    .build();
        }
    }

    @Override
    public boolean closePayment(String orderId) {
        try {
            String key = PAYMENT_ORDER_KEY_PREFIX + orderId;
            PaymentOrder order = (PaymentOrder) redisTemplate.opsForValue().get(key);

            if (order == null) {
                log.warn("关闭支付订单失败，订单不存在：{}", orderId);
                return false;
            }

            if (!PaymentStatus.PENDING.equals(order.getStatus())) {
                log.warn("只能关闭待支付的订单，当前状态：{}", order.getStatus());
                return false;
            }

            order.setStatus(PaymentStatus.CANCELLED);
            redisTemplate.opsForValue().set(key, order, 30, TimeUnit.MINUTES);

            log.info("关闭支付订单成功：{}", orderId);
            return true;

        } catch (Exception e) {
            log.error("关闭支付订单失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean refund(RefundRequest request) {
        try {
            String key = PAYMENT_ORDER_KEY_PREFIX + request.getOriginalOrderId();
            PaymentOrder order = (PaymentOrder) redisTemplate.opsForValue().get(key);

            if (order == null) {
                log.warn("退款失败，订单不存在：{}", request.getOriginalOrderId());
                return false;
            }

            if (!PaymentStatus.PAID.equals(order.getStatus())) {
                log.warn("只能退款已支付的订单，当前状态：{}", order.getStatus());
                return false;
            }

            // 调用对应的退款服务
            boolean success;
            if (PaymentMethod.WECHAT.equals(order.getPaymentMethod())) {
                success = weChatPayService.refund(request);
            } else if (PaymentMethod.ALIPAY.equals(order.getPaymentMethod())) {
                success = alipayService.refund(request);
            } else {
                log.warn("不支持的支付方式退款：{}", order.getPaymentMethod());
                return false;
            }

            if (success) {
                order.setStatus(PaymentStatus.REFUNDED);
                redisTemplate.opsForValue().set(key, order, 30, TimeUnit.MINUTES);
                log.info("退款成功，订单号：{}", request.getOriginalOrderId());
            }

            return success;

        } catch (Exception e) {
            log.error("退款失败：{}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public PaymentResponse queryRefund(String refundOrderId) {
        // TODO: 实现退款查询逻辑
        log.info("查询退款状态，退款订单号：{}", refundOrderId);
        return PaymentResponse.builder()
                .status(PaymentStatus.PENDING.getCode())
                .build();
    }

    /**
     * 生成订单号
     */
    private String generateOrderId() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
