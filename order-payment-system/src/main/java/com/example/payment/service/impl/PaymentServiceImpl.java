package com.example.payment.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.payment.dto.request.PayOrderRequest;
import com.example.payment.dto.response.PaymentResponse;
import com.example.payment.entity.Order;
import com.example.payment.entity.PaymentOrder;
import com.example.payment.enums.OrderStatus;
import com.example.payment.enums.PaymentMethod;
import com.example.payment.enums.PaymentStatus;
import com.example.payment.exception.BusinessException;
import com.example.payment.mapper.PaymentOrderMapper;
import com.example.payment.mq.PaymentMessageProducer;
import com.example.payment.service.OrderService;
import com.example.payment.service.PaymentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    private PaymentOrderMapper paymentOrderMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private PaymentMessageProducer paymentMessageProducer;

    @Value("${payment.timeout-minutes:30}")
    private Integer paymentTimeoutMinutes;

    @Value("${payment.prefix:PAY}")
    private String paymentPrefix;

    private static final String PAYMENT_CALLBACK_KEY = "payment:callback:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentResponse createPayment(PayOrderRequest request) {
        log.info("创建支付单，orderNo: {}, paymentMethod: {}", request.getOrderNo(), request.getPaymentMethod());

        // 1. 查询订单
        Order order = orderService.getEntityByOrderNo(request.getOrderNo());
        if (order == null) {
            throw new BusinessException("订单不存在");
        }

        OrderStatus orderStatus = OrderStatus.fromCode(order.getStatus());
        if (orderStatus != OrderStatus.PENDING_PAYMENT) {
            throw new BusinessException("订单状态不允许支付，当前状态：" + (orderStatus != null ? orderStatus.getDesc() : "未知"));
        }

        // 2. 检查是否已有待支付的支付单
        PaymentOrder existingPayment = getLatestPaymentByOrderNo(request.getOrderNo());
        if (existingPayment != null) {
            PaymentStatus existingStatus = PaymentStatus.fromCode(existingPayment.getStatus());
            if (existingStatus == PaymentStatus.PENDING || existingStatus == PaymentStatus.PAYING) {
                // 返回已有的支付单
                log.info("已存在待支付的支付单，paymentNo: {}", existingPayment.getPaymentNo());
                return PaymentResponse.fromEntity(existingPayment);
            }
        }

        // 3. 生成支付单号
        String paymentNo = generatePaymentNo();

        // 4. 创建支付单
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .paymentNo(paymentNo)
                .orderNo(request.getOrderNo())
                .userId(order.getUserId())
                .amount(order.getPayAmount())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.PENDING.getCode())
                .description(order.getProductName())
                .returnUrl(request.getReturnUrl())
                .expireTime(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes))
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .deleted(0)
                .build();

        paymentOrderMapper.insert(paymentOrder);

        log.info("支付单创建成功，paymentNo: {}", paymentNo);

        // 5. 调用第三方支付平台
        PaymentResponse response = invokeThirdPartyPayment(paymentOrder, request);

        // 6. 更新支付单信息
        PaymentOrder updatePayment = new PaymentOrder();
        updatePayment.setId(paymentOrder.getId());
        updatePayment.setPayUrl(response.getPayUrl());
        updatePayment.setPrepayId(response.getPrepayId());
        updatePayment.setStatus(PaymentStatus.PAYING.getCode());
        paymentOrderMapper.updateById(updatePayment);

        response.setPaymentNo(paymentNo);
        response.setOrderNo(request.getOrderNo());
        response.setAmount(order.getPayAmount());
        response.setExpireTime(paymentOrder.getExpireTime());
        response.setCreateTime(paymentOrder.getCreateTime());

        return response;
    }

    @Override
    public PaymentResponse queryPayment(String paymentNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getPaymentNo, paymentNo);
        PaymentOrder paymentOrder = paymentOrderMapper.selectOne(wrapper);
        return PaymentResponse.fromEntity(paymentOrder);
    }

    @Override
    public PaymentResponse queryPaymentByOrderNo(String orderNo) {
        PaymentOrder paymentOrder = getLatestPaymentByOrderNo(orderNo);
        return PaymentResponse.fromEntity(paymentOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean closePayment(String paymentNo) {
        log.info("关闭支付单，paymentNo: {}", paymentNo);

        int updated = paymentOrderMapper.updateStatusByPaymentNo(
                paymentNo,
                PaymentStatus.CLOSED.getCode(),
                PaymentStatus.PENDING.getCode()
        );

        if (updated > 0) {
            // TODO: 调用第三方支付平台关闭订单
            log.info("支付单关闭成功，paymentNo: {}", paymentNo);
            return true;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleWechatCallback(Map<String, String> params) {
        log.info("处理微信支付回调，params: {}", params);

        String paymentNo = params.get("out_trade_no");
        String transactionId = params.get("transaction_id");
        String resultCode = params.get("result_code");

        // 幂等性检查
        String callbackKey = PAYMENT_CALLBACK_KEY + paymentNo;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(callbackKey, "1", 24, TimeUnit.HOURS);
        if (isNew == null || !isNew) {
            log.info("回调已处理过，paymentNo: {}", paymentNo);
            return true;
        }

        try {
            if ("SUCCESS".equals(resultCode)) {
                // 更新支付单状态
                int updated = paymentOrderMapper.updatePaySuccess(
                        paymentNo,
                        transactionId,
                        JSON.toJSONString(params)
                );

                if (updated > 0) {
                    // 查询支付单获取订单号
                    PaymentResponse payment = queryPayment(paymentNo);
                    if (payment != null) {
                        // 发送MQ通知订单服务
                        paymentMessageProducer.sendPaySuccessMessage(payment.getOrderNo(), paymentNo);
                    }
                    log.info("微信支付回调处理成功，paymentNo: {}", paymentNo);
                    return true;
                }
            } else {
                // 支付失败
                paymentOrderMapper.updateStatusByPaymentNo(
                        paymentNo,
                        PaymentStatus.FAILED.getCode(),
                        PaymentStatus.PAYING.getCode()
                );
                log.warn("微信支付失败，paymentNo: {}", paymentNo);
            }
        } catch (Exception e) {
            // 处理失败，删除幂等标记，允许重试
            redisTemplate.delete(callbackKey);
            throw e;
        }

        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleAlipayCallback(Map<String, String> params) {
        log.info("处理支付宝回调，params: {}", params);

        String paymentNo = params.get("out_trade_no");
        String transactionId = params.get("trade_no");
        String tradeStatus = params.get("trade_status");

        // 幂等性检查
        String callbackKey = PAYMENT_CALLBACK_KEY + paymentNo;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(callbackKey, "1", 24, TimeUnit.HOURS);
        if (isNew == null || !isNew) {
            log.info("回调已处理过，paymentNo: {}", paymentNo);
            return true;
        }

        try {
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                // 更新支付单状态
                int updated = paymentOrderMapper.updatePaySuccess(
                        paymentNo,
                        transactionId,
                        JSON.toJSONString(params)
                );

                if (updated > 0) {
                    // 查询支付单获取订单号
                    PaymentResponse payment = queryPayment(paymentNo);
                    if (payment != null) {
                        // 发送MQ通知订单服务
                        paymentMessageProducer.sendPaySuccessMessage(payment.getOrderNo(), paymentNo);
                    }
                    log.info("支付宝回调处理成功，paymentNo: {}", paymentNo);
                    return true;
                }
            } else {
                log.warn("支付宝交易状态非成功，tradeStatus: {}", tradeStatus);
            }
        } catch (Exception e) {
            redisTemplate.delete(callbackKey);
            throw e;
        }

        return false;
    }

    @Override
    public PaymentResponse syncPaymentStatus(String paymentNo) {
        log.info("主动查询支付状态，paymentNo: {}", paymentNo);

        PaymentResponse payment = queryPayment(paymentNo);
        if (payment == null) {
            throw new BusinessException("支付单不存在");
        }

        // TODO: 调用第三方支付平台查询真实状态
        // 这里模拟查询结果
        
        return payment;
    }

    /**
     * 获取订单最新的支付单
     */
    private PaymentOrder getLatestPaymentByOrderNo(String orderNo) {
        LambdaQueryWrapper<PaymentOrder> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PaymentOrder::getOrderNo, orderNo)
                .orderByDesc(PaymentOrder::getCreateTime)
                .last("LIMIT 1");
        return paymentOrderMapper.selectOne(wrapper);
    }

    /**
     * 调用第三方支付平台
     */
    private PaymentResponse invokeThirdPartyPayment(PaymentOrder paymentOrder, PayOrderRequest request) {
        PaymentMethod method = PaymentMethod.fromCode(request.getPaymentMethod());
        
        PaymentResponse response = PaymentResponse.builder()
                .paymentMethod(request.getPaymentMethod())
                .paymentMethodDesc(method != null ? method.getDesc() : "未知")
                .status(PaymentStatus.PAYING.getCode())
                .statusDesc(PaymentStatus.PAYING.getDesc())
                .build();

        if (method == PaymentMethod.WECHAT) {
            // 模拟微信支付预下单
            response.setPayUrl("weixin://wxpay/bizpayurl?pr=xxx");
            response.setPrepayId("wx_prepay_" + IdUtil.fastSimpleUUID());
            
            // 构建JSAPI支付参数
            PaymentResponse.WechatPayParams wechatParams = PaymentResponse.WechatPayParams.builder()
                    .appId("your_app_id")
                    .timeStamp(String.valueOf(System.currentTimeMillis() / 1000))
                    .nonceStr(IdUtil.fastSimpleUUID())
                    .packageValue("prepay_id=" + response.getPrepayId())
                    .signType("RSA")
                    .paySign("mock_sign")
                    .build();
            response.setWechatPayParams(wechatParams);
            
        } else if (method == PaymentMethod.ALIPAY) {
            // 模拟支付宝预下单
            response.setPayUrl("https://openapi.alipay.com/gateway.do?xxx");
            response.setAlipayForm("<form id='alipaysubmit'>...</form>");
        } else {
            throw new BusinessException("不支持的支付方式");
        }

        log.info("调用第三方支付平台成功，paymentMethod: {}", method);
        return response;
    }

    /**
     * 生成支付单号
     */
    private String generatePaymentNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = IdUtil.fastSimpleUUID().substring(0, 6).toUpperCase();
        return paymentPrefix + date + random;
    }
}

