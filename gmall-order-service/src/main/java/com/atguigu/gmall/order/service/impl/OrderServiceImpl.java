package com.atguigu.gmall.order.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.OmsOrder;
import com.atguigu.gmall.bean.OmsOrderItem;
import com.atguigu.gmall.order.mapper.OrderItemMapper;
import com.atguigu.gmall.order.mapper.OrderMapper;
import com.atguigu.gmall.service.OrderService;
import com.atguigu.gmall.util.ActiveMQUtil;
import com.atguigu.gmall.util.RedisUtil;
import org.apache.activemq.command.ActiveMQTextMessage;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;
import tk.mybatis.mapper.entity.Example;

import javax.jms.*;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    OrderMapper orderMapper;

    @Autowired
    OrderItemMapper orderItemMapper;

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Override
    public boolean checkTradeCode(String memberId, String tradeCode) {
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";
//            String tradeCodeFromCache = jedis.get(tradeKey);
            // 交易码使用一次就删除，使用lua脚本在发现key的同时将key删除，防止并发订单攻击（订单重复提交）
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            Long eval = (Long)jedis.eval(script, Collections.singletonList(tradeKey), Collections.singletonList(tradeCode));
            if (eval != null && eval != 0) {
//                jedis.del(tradeKey);
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedis.close();
        }
    }

    @Override
    public String generateTradeCode(String memberId) {
        // redis中key的存放结构 user:memeberId:tradeCode
        Jedis jedis = null;
        try {
            jedis = redisUtil.getJedis();
            String tradeKey = "user:" + memberId + ":tradeCode";
            String tradeCode = UUID.randomUUID().toString();
            jedis.setex(tradeKey, 60*15, tradeCode);
            return tradeCode;

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return null;
    }

    @Override
    public String saveOrder(OmsOrder order) {

        // 保存订单表
        orderMapper.insertSelective(order);
        String orderId = order.getId();
        // 保存订单详情
        List<OmsOrderItem> orderItems = order.getOmsOrderItems();
        for (OmsOrderItem orderItem : orderItems) {
            orderItem.setOrderId(orderId);
            orderItemMapper.insertSelective(orderItem);
            // 删除购物车数据
//            cartService.delBySkuId();
        }
        return orderId;
    }

    @Override
    public OmsOrder getOrderById(String orderId) {

        OmsOrder order = new OmsOrder();
        order.setId(orderId);
        return orderMapper.selectOne(order);
    }

    @Override
    public OmsOrder getOrderByOutTreadeNo(String outTradeNo) {
        OmsOrder order = new OmsOrder();
        order.setOrderSn(outTradeNo);
        return orderMapper.selectOne(order);
    }

    /**
     * 支付成功后更新订单的支付状态，改成已支付 1表示已支付
     * @param order
     */
    @Override
    public void updateOrder(OmsOrder order) {
        Example e = new Example(OmsOrder.class);
        e.createCriteria().andEqualTo("orderSn", order.getOrderSn());

        // 发送一个订单已支付的队列，提供给库存消费
        Connection connection = null;
        Session session = null;
        try {
            // 先跟新订单的状态
            orderMapper.updateByExampleSelective(order, e);

            connection = activeMQUtil.getConnectionFactory().createConnection();
            session = connection.createSession(true, Session.SESSION_TRANSACTED);

            // 调用mq发送定单已支付的消息
            Queue paymentSuccessQueue = session.createQueue("ORDER_PAY_QUEQUE");
            MessageProducer producer = session.createProducer(paymentSuccessQueue);
            ActiveMQTextMessage textMessage = new ActiveMQTextMessage();
//            ActiveMQMapMessage mapMessage = new ActiveMQMapMessage(); // hash 结构
            // 查询订单对象，转化为JSON字符串存入消息队列
            OmsOrder orderParam = new OmsOrder();
            orderParam.setOrderSn(order.getOrderSn());
            OmsOrder orderResponse = orderMapper.selectOne(orderParam);

            // 根据订单号查询订单细节（有可能一个订单里面有多个商品）
            OmsOrderItem item = new OmsOrderItem();
            item.setOrderSn(order.getOrderSn());
            List<OmsOrderItem> orderItems = orderItemMapper.select(item);
            orderResponse.setOmsOrderItems(orderItems);

            // 把订单作为消息发送给MQ，让库存系统去消费，进行后续的工作
            textMessage.setText(JSON.toJSONString(orderResponse));
            producer.send(textMessage);
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

}
