package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.PaymentInfo;

import java.util.Map;

public interface PaymentService {

    void savePaymentInfo(PaymentInfo paymentInfo);

    void updatePaymentInfo(PaymentInfo paymentInfo);

    void checkPaymentResultQueue(String outTradeNo, int count);

    Map<String, Object> alipayQuery(String outTradeNo);
}
