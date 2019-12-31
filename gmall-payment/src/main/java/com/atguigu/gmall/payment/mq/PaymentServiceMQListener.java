package com.atguigu.gmall.payment.mq;

import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.service.PaymentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.jms.JMSException;
import javax.jms.MapMessage;
import java.util.Date;
import java.util.Map;

@Component
public class PaymentServiceMQListener {

    @Autowired
    PaymentService paymentService;

    @JmsListener(destination = "PAYMENT_CHECK_QUEUE", containerFactory = "jmsQueueListener")
    public void consumePaymentCheckResult (MapMessage mapMessage) throws JMSException {
        String outTradeNo = mapMessage.getString("out_trade_no");
        int count = 0;
        if (mapMessage.getInt("count") >= 0) {
            count = mapMessage.getInt("count");
        }

        // 调用paymentService的支付宝检查接口 alipay.trade.query 统一收单线下交易查询接口
        Map<String, Object> alipayResultMap = paymentService.alipayQuery(outTradeNo);
        if (alipayResultMap != null && !alipayResultMap.isEmpty()) {
            String tradeStatus = (String) alipayResultMap.get("trade_status");
            // 根据查询的支付状态结果，判断是否进行下一次的延迟任务还是支付成功更新数据和后续任务
            if (StringUtils.isNotBlank(tradeStatus) && tradeStatus.equals("TRADE_SUCCESS")) {
                // 支付成功呢，更新支付发送支付队列
                System.out.println("已经支付成功，调用支付服务，修改支付信息发送支付成功的队列");

                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setOutTradeNo(outTradeNo);
                paymentInfo.setPaymentStatus("已支付"); //  已支付
                paymentInfo.setAlipayTradeNo((String)alipayResultMap.get("trade_no"));  // 支付宝的交易凭证号
                paymentInfo.setCallbackContent((String)alipayResultMap.get("call_back_content")); // 回调请求字符串
                paymentInfo.setCallbackTime(new Date());
                paymentService.updatePaymentInfo(paymentInfo);

                return;
            }
        }

        if (count > 0) {
            // 如果查询没有得到结果，那么继续发送延迟检查任务
            System.out.println("alipayResultMap没有结果,检查次数为：" + count + "，继续发送延迟检查");
            count--;
            paymentService.checkPaymentResultQueue(outTradeNo, count);
        } else {
            System.out.println("检查次数用尽，结束主动检查是否支付");
        }

    }
}
