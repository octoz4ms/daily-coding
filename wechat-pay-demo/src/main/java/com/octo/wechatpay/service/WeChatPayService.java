package com.octo.wechatpay.service;

import com.octo.wechatpay.config.WeChatPayProperties;
import com.octo.wechatpay.dto.NativePrepayRequest;
import com.octo.wechatpay.dto.NativePrepayResponse;
import com.octo.wechatpay.dto.OrderQueryResponse;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.service.payments.nativepay.NativePayService;
import com.wechat.pay.java.service.payments.nativepay.model.Amount;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayRequest;
import com.wechat.pay.java.service.payments.nativepay.model.PrepayResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 微信支付服务 - Native 支付标准流程
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeChatPayService {

    private final NativePayService nativePayService;
    private final WeChatPayProperties weChatPayProperties;

    /**
     * Native 下单 - 生成支付二维码
     */
    public NativePrepayResponse nativePrepay(NativePrepayRequest request) {
        PrepayRequest prepayRequest = new PrepayRequest();
        prepayRequest.setAppid(weChatPayProperties.getAppId());
        prepayRequest.setMchid(weChatPayProperties.getMchId());
        prepayRequest.setDescription(request.getDescription());
        prepayRequest.setOutTradeNo(request.getOutTradeNo());
        prepayRequest.setNotifyUrl(weChatPayProperties.getNotifyUrl());
        if (request.getAttach() != null && !request.getAttach().isEmpty()) {
            prepayRequest.setAttach(request.getAttach());
        }

        // 金额：元转分
        Amount amount = new Amount();
        amount.setTotal(request.getTotalAmount().multiply(new BigDecimal("100")).setScale(0, RoundingMode.DOWN).intValue());
        amount.setCurrency("CNY");
        prepayRequest.setAmount(amount);

        PrepayResponse response = nativePayService.prepay(prepayRequest);
        log.info("Native 下单成功，订单号: {}, codeUrl: {}", request.getOutTradeNo(), response.getCodeUrl());

        return NativePrepayResponse.builder()
                .outTradeNo(request.getOutTradeNo())
                .codeUrl(response.getCodeUrl())
                .prepayId(null)  // Native 支付仅返回 code_url，无 prepay_id
                .build();
    }

    /**
     * 查询订单 - 根据商户订单号
     */
    public OrderQueryResponse queryOrderByOutTradeNo(String outTradeNo) {
        var queryRequest = new com.wechat.pay.java.service.payments.nativepay.model.QueryOrderByOutTradeNoRequest();
        queryRequest.setMchid(weChatPayProperties.getMchId());
        queryRequest.setOutTradeNo(outTradeNo);

        Transaction transaction = nativePayService.queryOrderByOutTradeNo(queryRequest);
        log.info("查询订单成功，订单号: {}, 状态: {}", outTradeNo, transaction.getTradeState());

        String tradeState = transaction.getTradeState() != null ? transaction.getTradeState().name() : null;
        return OrderQueryResponse.builder()
                .outTradeNo(transaction.getOutTradeNo())
                .transactionId(transaction.getTransactionId())
                .tradeState(tradeState)
                .tradeStateDesc(tradeState)
                .successTime(transaction.getSuccessTime() != null ? transaction.getSuccessTime() : null)
                .build();
    }

    /**
     * 关闭订单
     */
    public void closeOrder(String outTradeNo) {
        var closeRequest = new com.wechat.pay.java.service.payments.nativepay.model.CloseOrderRequest();
        closeRequest.setMchid(weChatPayProperties.getMchId());
        closeRequest.setOutTradeNo(outTradeNo);

        nativePayService.closeOrder(closeRequest);
        log.info("关闭订单成功，订单号: {}", outTradeNo);
    }
}
