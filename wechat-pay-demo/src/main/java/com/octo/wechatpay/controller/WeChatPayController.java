package com.octo.wechatpay.controller;

import com.octo.wechatpay.dto.NativePrepayRequest;
import com.octo.wechatpay.dto.NativePrepayResponse;
import com.octo.wechatpay.dto.OrderQueryResponse;
import com.octo.wechatpay.service.WeChatPayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 微信支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat/pay")
@RequiredArgsConstructor
public class WeChatPayController {

    private final WeChatPayService weChatPayService;

    /**
     * Native 下单 - 生成支付二维码
     */
    @PostMapping("/native/prepay")
    public ResponseEntity<NativePrepayResponse> nativePrepay(@Valid @RequestBody NativePrepayRequest request) {
        log.info("Native 下单请求: {}", request);
        NativePrepayResponse response = weChatPayService.nativePrepay(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 查询订单
     */
    @GetMapping("/order/{outTradeNo}")
    public ResponseEntity<OrderQueryResponse> queryOrder(@PathVariable String outTradeNo) {
        log.info("查询订单: {}", outTradeNo);
        OrderQueryResponse response = weChatPayService.queryOrderByOutTradeNo(outTradeNo);
        return ResponseEntity.ok(response);
    }

    /**
     * 关闭订单
     */
    @PostMapping("/order/{outTradeNo}/close")
    public ResponseEntity<Void> closeOrder(@PathVariable String outTradeNo) {
        log.info("关闭订单: {}", outTradeNo);
        weChatPayService.closeOrder(outTradeNo);
        return ResponseEntity.noContent().build();
    }
}
