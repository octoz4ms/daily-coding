package com.octo.wechatpay.controller;

import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.exception.ValidationException;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 微信支付回调通知控制器
 * 重要：必须使用原始请求体验签，不能使用 JSON 反序列化后的字符串
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat/pay")
@RequiredArgsConstructor
public class WeChatPayNotifyController {

    private final NotificationConfig notificationConfig;

    /**
     * 支付结果通知
     * 文档：https://pay.weixin.qq.com/docs/merchant/development/interface-rules/result-notification.html
     */
    @PostMapping("/notify")
    public ResponseEntity<Map<String, String>> payNotify(
            HttpServletRequest request,
            @RequestBody String requestBody) {

        log.info("收到微信支付回调，body 长度: {}", requestBody != null ? requestBody.length() : 0);

        try {
            // 构建验签所需参数 - 必须使用原始 body
            RequestParam requestParam = new RequestParam.Builder()
                    .serialNumber(request.getHeader("Wechatpay-Serial"))
                    .nonce(request.getHeader("Wechatpay-Nonce"))
                    .signature(request.getHeader("Wechatpay-Signature"))
                    .timestamp(request.getHeader("Wechatpay-Timestamp"))
                    .signType(request.getHeader("Wechatpay-Signature-Type"))
                    .body(requestBody)
                    .build();

            NotificationParser parser = new NotificationParser(notificationConfig);
            Transaction transaction = parser.parse(requestParam, Transaction.class);

            log.info("支付回调验签成功，订单号: {}, 微信订单号: {}, 状态: {}",
                    transaction.getOutTradeNo(),
                    transaction.getTransactionId(),
                    transaction.getTradeState());

            // 业务处理：更新订单状态、发货等
            handlePaySuccess(transaction);

            // 返回 200 表示处理成功，微信将不再重试
            return ResponseEntity.ok(Map.of("code", "SUCCESS", "message", "成功"));

        } catch (ValidationException e) {
            log.error("支付回调验签失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 处理支付成功业务逻辑
     */
    private void handlePaySuccess(Transaction transaction) {
        // TODO: 根据 out_trade_no 更新数据库订单状态
        // TODO: 发送支付成功通知
        // TODO: 触发后续业务流程（发货、积分等）
        log.info("支付成功，执行业务逻辑 - 商户订单号: {}, 金额: {} 分",
                transaction.getOutTradeNo(),
                transaction.getAmount() != null ? transaction.getAmount().getTotal() : 0);
    }
}
