package com.msi.tough.monitor.connector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.transaction.TransactionConfiguration;

import com.msi.tough.core.Appctx;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties.Builder;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

//@Ignore("Send a test message to local queue.")
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/test-monitor-context.xml" })
@TransactionConfiguration(transactionManager = "transactionManager", defaultRollback = false)
@TestExecutionListeners({ DependencyInjectionTestExecutionListener.class })
public class MessageAgentSender {

    private static final Logger logger = Appctx
            .getLogger(MessageAgentSender.class.getName());

    private static final String EXCHANGE_NAME = "amq.fanout";

    @Test
    public void sendAMessage() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setVirtualHost("/monitor");
        factory.setUsername("transcend");
        factory.setPassword("transcend");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();

        //channel.exchangeDeclare(EXCHANGE_NAME, "fanout");

        String message = "[{}]";

        Builder propBuilder = new AMQP.BasicProperties.Builder()
            .contentType("json");
        channel.basicPublish(EXCHANGE_NAME, "", propBuilder.build(), message.getBytes());
        logger.info(" [x] Sent '" + message + "'");

        channel.close();
        connection.close();
    }

}
