package com.octo.wechatpay.controller;

import com.octo.wechatpay.dto.JsapiPrepayRequest;
import com.octo.wechatpay.dto.JsapiPrepayResponse;
import com.octo.wechatpay.service.JsapiPayService;
import com.octo.wechatpay.service.WeChatJsapiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * JSAPI 支付控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/wechat")
@RequiredArgsConstructor
public class JsapiController {

    private final WeChatJsapiService weChatJsapiService;
    private final JsapiPayService jsapiPayService;

    /**
     * 获取 wx.config 所需参数（用于页面加载时初始化）
     */
    @GetMapping("/jsapi/config")
    public ResponseEntity<Map<String, Object>> getJsapiConfig(@RequestParam String url) {
        log.info("获取 JSAPI config，url: {}", url);
        Map<String, Object> config = weChatJsapiService.getJsapiConfig(url);
        return ResponseEntity.ok(config);
    }

    /**
     * JSAPI 下单 - 返回调起支付参数
     */
    @PostMapping("/pay/jsapi/prepay")
    public ResponseEntity<JsapiPrepayResponse> jsapiPrepay(@Valid @RequestBody JsapiPrepayRequest request) {
        log.info("JSAPI 下单请求: {}", request);
        JsapiPrepayResponse response = jsapiPayService.jsapiPrepay(request);
        return ResponseEntity.ok(response);
    }
}
