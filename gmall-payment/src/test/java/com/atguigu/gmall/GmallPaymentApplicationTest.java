package com.atguigu.gmall;

import com.atguigu.gmall.util.ActiveMQUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.test.context.junit4.SpringRunner;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class GmallPaymentApplicationTest {

    @Autowired
    ActiveMQUtil activeMQUtil;

    @Autowired
    DefaultJmsListenerContainerFactory jmsQueueListener;

    @Test
    public void testConnection() throws JMSException {
        ConnectionFactory connectionFactory = activeMQUtil.getConnectionFactory();
        Connection connection = connectionFactory.createConnection();
        System.out.println(connection);
    }
}
