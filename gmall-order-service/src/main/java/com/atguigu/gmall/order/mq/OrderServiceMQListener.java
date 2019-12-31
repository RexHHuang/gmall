package com.atguigu.gmall.order.mq;

import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;

@Component
public class OrderServiceMQListener {

    @Autowired
    OrderService orderService;

    /**
     * 这个queue存在，说明已经支付成功，马上更新订单的状态
     * @param mapMessage
     */
    @JmsListener(destination = "PAYMENT_SUCCESS_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentResult (MapMessage mapMessage) throws JMSException {

        String outTradeNo = mapMessage.getString("out_trade_no");
        // 更新订单状态
        OmsOrder order = new OmsOrder();
        order.setOrderSn(outTradeNo);
        order.setStatus("1");
        orderService.updateOrder(order);

    }
}
