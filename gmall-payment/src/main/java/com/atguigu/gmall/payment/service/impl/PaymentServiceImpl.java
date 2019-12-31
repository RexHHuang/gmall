package com.atguigu.gmall.payment.service.impl;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.atguigu.gmall.bean.PaymentInfo;
import com.atguigu.gmall.payment.mapper.PaymentInfoMapper;
import com.atguigu.gmall.service.PaymentService;
import com.atguigu.gmall.util.ActiveMQUtil;
import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQMapMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.HashMap;
import java.util.Map;

@Service
public class PaymentServiceImpl implements PaymentService {

    @Autowired
    PaymentInfoMapper paymentInfoMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    AlipayClient alipayClient;

    @Override
    public void savePaymentInfo(PaymentInfo paymentInfo) {
        paymentInfoMapper.insertSelective(paymentInfo);
    }

    @Override
    public void updatePaymentInfo(PaymentInfo paymentInfo) {

        // 进行支付更新的幂等性检查，防止重复更新
        PaymentInfo info = new PaymentInfo();
        info.setOutTradeNo(paymentInfo.getOutTradeNo());
        PaymentInfo result = paymentInfoMapper.selectOne(info);
        if (StringUtils.isNotBlank(result.getPaymentStatus()) && result.getPaymentStatus().equals(paymentInfo.getPaymentStatus())) {
            return;
        }

        Example e = new Example(PaymentInfo.class);
        e.createCriteria().andEqualTo("outTradeNo", paymentInfo.getOutTradeNo());

        Connection connection = null;
        Session session = null;
        try {
            paymentInfoMapper.updateByExampleSelective(paymentInfo, e);

            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            // 调用mq发送支付成功的消息，给其他业务（如订单业务消费，去更新订单的状态）
            Queue paymentSuccessQueue = session.createQueue("PAYMENT_SUCCESS_QUEUE");
            MessageProducer producer = session.createProducer(paymentSuccessQueue);

//            TextMessage textMessage = new ActiveMQTextMessage();  // 字符串文本
            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage(); // hash 结构
            mapMessage.setString("out_trade_no", paymentInfo.getOutTradeNo());

            producer.send(mapMessage);
            session.commit();
        } catch (Exception ex) {
            // 消息回滚
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        }
    }

    /**
     * 商家主动发出检查是否支付的延迟队列
     * @param outTradeNo
     */
    @Override
    public void checkPaymentResultQueue(String outTradeNo, int count) {
        Connection connection = null;
        Session session = null;
        try {
            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);
            Queue paymentCheckQueue = session.createQueue("PAYMENT_CHECK_QUEUE");
            MessageProducer producer = session.createProducer(paymentCheckQueue);

            MapMessage mapMessage = new ActiveMQMapMessage();
            mapMessage.setString("out_trade_no", outTradeNo);
            mapMessage.setInt("count", count);
            // 设置延迟发送时间
            mapMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, 1000*60);
            producer.send(mapMessage);
            session.commit();
        } catch (JMSException e) {
            try {
                session.rollback();
            } catch (JMSException e1) {
                e1.printStackTrace();
            }
        } finally {
            try {
                connection.close();
            } catch (JMSException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 阿里的支付查询接口
     * @param outTradeNo
     * @return
     */
    @Override
    public Map<String, Object> alipayQuery(String outTradeNo) {

        Map<String, Object> alipayResultMap = new HashMap<>();

        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();

        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("out_trade_no", outTradeNo);
        String requestStr = JSON.toJSONString(requestMap);
        request.setBizContent(requestStr);

        AlipayTradeQueryResponse response = null;
        try {

            response = alipayClient.execute(request);
            if (response.isSuccess()) {
                System.out.println("交易已创建，调用成功");
                alipayResultMap.put("out_trade_no", outTradeNo);
                alipayResultMap.put("trade_no", response.getTradeNo());
                alipayResultMap.put("trade_status", response.getTradeStatus());
                alipayResultMap.put("call_back_content", response.getMsg());
            } else {
                System.out.println("有可能交易未创建，调用失败");
            }
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return alipayResultMap;
    }
}
