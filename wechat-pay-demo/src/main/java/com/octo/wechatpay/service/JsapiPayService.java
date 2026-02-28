package com.octo.wechatpay.service;

import com.octo.wechatpay.config.WeChatPayProperties;
import com.octo.wechatpay.dto.JsapiPrepayRequest;
import com.octo.wechatpay.dto.JsapiPrepayResponse;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * JSAPI 支付服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsapiPayService {

    private final JsapiServiceExtension jsapiServiceExtension;
    private final WeChatPayProperties weChatPayProperties;

    /**
     * JSAPI 下单 - 返回调起支付参数
     */
    public JsapiPrepayResponse jsapiPrepay(JsapiPrepayRequest request) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(weChatPayProperties.getAppId());
        prepayRequest.setMchid(weChatPayProperties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(request.getOutTradeNo());
        prepayRequest.setNotifyUrl(weChatPayProperties.getNotifyUrl());
        if (request.getAttach() != null && !request.getAttach().isEmpty()) {
            prepayRequest.setAttach(request.getAttach());
        }

        Amount amount = new Amount();
        amount.setTotal(request.getTotalAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.DOWN).intValue());
        amount.setCurrency("CNY");
        prepayRequest.setAmount(amount);

        Payer payer = new Payer();
        payer.setOpenid(request.getOpenid());
        prepayRequest.setPayer(payer);

        PrepayWithRequestPaymentResponse response = jsapiServiceExtension.prepayWithRequestPayment(prepayRequest);
        log.info("JSAPI 下单成功，订单号: {}", request.getOutTradeNo());

        return JsapiPrepayResponse.builder()
                .timeStamp(response.getTimeStamp())
                .nonceStr(response.getNonceStr())
                .packageVal(response.getPackageVal())
                .signType(response.getSignType())
                .paySign(response.getPaySign())
                .build();
    }
}
