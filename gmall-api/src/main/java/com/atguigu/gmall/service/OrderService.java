package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.OmsOrder;

import java.math.BigDecimal;

public interface OrderService {

    boolean checkTradeCode(String memberId, String tradeCode);

    String generateTradeCode(String memberId);

    String saveOrder(OmsOrder order);

    OmsOrder getOrderById(String orderId);

    OmsOrder getOrderByOutTreadeNo(String outTradeNo);

    void updateOrder(OmsOrder order);
}
