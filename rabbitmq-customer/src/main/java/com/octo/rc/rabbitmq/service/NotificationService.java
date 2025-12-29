package com.octo.rc.rabbitmq.service;

import com.octo.rc.rabbitmq.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 通知服务
 * 负责发送各种订单相关通知（短信、邮件、站内信等）
 */
@Service
@Slf4j
public class NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送订单创建通知
     *
     * @param order 订单信息
     */
    public void sendOrderCreatedNotification(Order order) {
        String message = String.format(
                "【订单创建成功】尊敬的用户 %s，您的订单 %s 已创建成功，" +
                        "商品：%s，金额：%.2f 元。请在30分钟内完成支付，逾期订单将自动取消。",
                order.getUserId(),
                order.getOrderId(),
                order.getProductName(),
                order.getAmount()
        );

        // 模拟发送短信
        sendSms(order.getUserId(), message);
        // 模拟发送邮件
        sendEmail(order.getUserId(), "订单创建成功通知", message);
        // 模拟发送站内信
        sendInAppMessage(order.getUserId(), message);

        log.info("订单创建通知已发送，订单ID: {}, 用户ID: {}", order.getOrderId(), order.getUserId());
    }

    /**
     * 发送订单超时取消通知
     *
     * @param order 订单信息
     */
    public void sendOrderCancelledNotification(Order order) {
        String message = String.format(
                "【订单已取消】尊敬的用户 %s，您的订单 %s 因超时未支付已被自动取消。" +
                        "商品：%s，金额：%.2f 元。如需购买，请重新下单。",
                order.getUserId(),
                order.getOrderId(),
                order.getProductName(),
                order.getAmount()
        );

        // 模拟发送短信
        sendSms(order.getUserId(), message);
        // 模拟发送邮件
        sendEmail(order.getUserId(), "订单超时取消通知", message);
        // 模拟发送站内信
        sendInAppMessage(order.getUserId(), message);

        log.info("订单取消通知已发送，订单ID: {}, 用户ID: {}", order.getOrderId(), order.getUserId());
    }

    /**
     * 发送订单支付成功通知
     *
     * @param order 订单信息
     */
    public void sendOrderPaidNotification(Order order) {
        String message = String.format(
                "【支付成功】尊敬的用户 %s，您的订单 %s 已支付成功！" +
                        "商品：%s，金额：%.2f 元，支付时间：%s。感谢您的购买！",
                order.getUserId(),
                order.getOrderId(),
                order.getProductName(),
                order.getAmount(),
                order.getPayTime().format(DATE_FORMATTER)
        );

        sendSms(order.getUserId(), message);
        sendEmail(order.getUserId(), "订单支付成功通知", message);
        sendInAppMessage(order.getUserId(), message);

        log.info("订单支付通知已发送，订单ID: {}, 用户ID: {}", order.getOrderId(), order.getUserId());
    }

    /**
     * 发送订单即将超时提醒
     *
     * @param order            订单信息
     * @param remainingMinutes 剩余分钟数
     */
    public void sendOrderExpiringSoonNotification(Order order, int remainingMinutes) {
        String message = String.format(
                "【支付提醒】尊敬的用户 %s，您的订单 %s 将在 %d 分钟后超时取消。" +
                        "商品：%s，金额：%.2f 元。请尽快完成支付！",
                order.getUserId(),
                order.getOrderId(),
                remainingMinutes,
                order.getProductName(),
                order.getAmount()
        );

        sendSms(order.getUserId(), message);
        sendInAppMessage(order.getUserId(), message);

        log.info("订单即将超时提醒已发送，订单ID: {}, 用户ID: {}, 剩余时间: {}分钟",
                order.getOrderId(), order.getUserId(), remainingMinutes);
    }

    /**
     * 模拟发送短信
     */
    private void sendSms(String userId, String message) {
        // 实际项目中，这里会调用短信服务商API（如阿里云短信、腾讯云短信等）
        log.info("📱 [短信] 发送至用户 {}: {}", userId, truncateMessage(message, 50));
    }

    /**
     * 模拟发送邮件
     */
    private void sendEmail(String userId, String subject, String content) {
        // 实际项目中，这里会调用邮件服务（如 JavaMailSender）
        log.info("📧 [邮件] 发送至用户 {}, 主题: {}", userId, subject);
    }

    /**
     * 模拟发送站内信
     */
    private void sendInAppMessage(String userId, String message) {
        // 实际项目中，这里会将消息存入数据库，并通过 WebSocket 推送给用户
        log.info("💬 [站内信] 发送至用户 {}: {}", userId, truncateMessage(message, 50));
    }

    /**
     * 截断消息用于日志显示
     */
    private String truncateMessage(String message, int maxLength) {
        if (message.length() <= maxLength) {
            return message;
        }
        return message.substring(0, maxLength) + "...";
    }
}




