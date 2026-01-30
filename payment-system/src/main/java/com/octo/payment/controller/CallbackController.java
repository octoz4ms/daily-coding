package com.octo.payment.controller;

import com.octo.payment.dto.PaymentCallback;
import com.octo.payment.entity.PaymentOrder;
import com.octo.payment.enums.PaymentMethod;
import com.octo.payment.enums.PaymentStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 支付回调控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/callback")
public class CallbackController {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String PAYMENT_ORDER_KEY_PREFIX = "payment:order:";

    /**
     * 微信支付回调
     */
    @PostMapping("/wechat")
    public ResponseEntity<String> wechatCallback(HttpServletRequest request,
                                                @RequestBody String body) {
        log.info("收到微信支付回调：{}", body);

        try {
            // TODO: 验证微信回调签名
            // 这里应该解析微信回调数据并验证签名

            // 模拟处理回调
            Map<String, String> params = parseWechatCallback(body);
            String orderId = params.get("out_trade_no");
            String transactionId = params.get("transaction_id");
            String tradeStatus = params.get("trade_status");

            PaymentCallback callback = PaymentCallback.builder()
                    .orderId(orderId)
                    .transactionId(transactionId)
                    .paymentMethod(PaymentMethod.WECHAT.getCode())
                    .status("SUCCESS".equals(tradeStatus) ? PaymentStatus.PAID.getCode() : PaymentStatus.FAILED.getCode())
                    .paidTime(LocalDateTime.now())
                    .rawData(body)
                    .processed(true)
                    .processTime(LocalDateTime.now())
                    .build();

            processCallback(callback);

            // 返回成功响应给微信
            return ResponseEntity.ok("<xml><return_code><![CDATA[SUCCESS]]></return_code><return_msg><![CDATA[OK]]></return_msg></xml>");

        } catch (Exception e) {
            log.error("处理微信支付回调失败：{}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("<xml><return_code><![CDATA[FAIL]]></return_code><return_msg><![CDATA[处理失败]]></return_msg></xml>");
        }
    }

    /**
     * 支付宝回调
     */
    @PostMapping("/alipay")
    public ResponseEntity<String> alipayCallback(HttpServletRequest request) {
        log.info("收到支付宝回调");

        try {
            // 获取支付宝POST过来反馈信息
            Map<String, String> params = new HashMap<>();
            Map<String, String[]> requestParams = request.getParameterMap();
            for (String key : requestParams.keySet()) {
                String[] values = requestParams.get(key);
                String valueStr = "";
                for (int i = 0; i < values.length; i++) {
                    valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
                }
                params.put(key, valueStr);
            }

            log.info("支付宝回调参数：{}", params);

            // TODO: 验证支付宝回调签名
            // 这里应该验证支付宝回调签名

            String orderId = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");

            PaymentCallback callback = PaymentCallback.builder()
                    .orderId(orderId)
                    .transactionId(tradeNo)
                    .paymentMethod(PaymentMethod.ALIPAY.getCode())
                    .status("TRADE_SUCCESS".equals(tradeStatus) ? PaymentStatus.PAID.getCode() : PaymentStatus.FAILED.getCode())
                    .paidTime(LocalDateTime.now())
                    .rawData(params.toString())
                    .processed(true)
                    .processTime(LocalDateTime.now())
                    .build();

            processCallback(callback);

            // 返回成功响应给支付宝
            return ResponseEntity.ok("success");

        } catch (Exception e) {
            log.error("处理支付宝回调失败：{}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("fail");
        }
    }

    /**
     * 处理回调
     */
    private void processCallback(PaymentCallback callback) {
        try {
            String key = PAYMENT_ORDER_KEY_PREFIX + callback.getOrderId();
            PaymentOrder order = (PaymentOrder) redisTemplate.opsForValue().get(key);

            if (order == null) {
                log.warn("回调处理失败，订单不存在：{}", callback.getOrderId());
                return;
            }

            // 更新订单状态
            if (PaymentStatus.PAID.getCode().equals(callback.getStatus())) {
                order.setStatus(PaymentStatus.PAID);
                order.setPaidTime(callback.getPaidTime());
                order.setTransactionId(callback.getTransactionId());
            } else if (PaymentStatus.FAILED.getCode().equals(callback.getStatus())) {
                order.setStatus(PaymentStatus.FAILED);
            }

            redisTemplate.opsForValue().set(key, order, 30, java.util.concurrent.TimeUnit.MINUTES);

            log.info("支付回调处理成功，订单号：{}，状态：{}", callback.getOrderId(), callback.getStatus());

            // TODO: 执行业务逻辑，如更新数据库、发送通知等

        } catch (Exception e) {
            log.error("处理支付回调失败：{}", e.getMessage(), e);
        }
    }

    /**
     * 解析微信回调数据（简化版）
     */
    private Map<String, String> parseWechatCallback(String xml) {
        // TODO: 实现XML解析逻辑
        Map<String, String> params = new HashMap<>();
        // 简化处理，实际应该解析XML
        params.put("out_trade_no", "PAY123456789");
        params.put("transaction_id", "420000123456789");
        params.put("trade_status", "SUCCESS");
        return params;
    }
}
