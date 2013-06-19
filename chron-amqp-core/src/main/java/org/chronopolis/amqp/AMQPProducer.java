/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.amqp;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import org.chronopolis.messaging.base.ChronMessage2;

/**
 * Note: This totally does not work... yet.
 *
 * @author shake
 */
public class AMQPProducer {
    private String virtualHost;
    private String host;
    private String exchange;
    private static final String exchangeType = "topic";
    // 2 == Persistent
    private static final Integer deliveryMode = 2;
    
    /**
     *
     * @param vhost
     * @param amqp
     * @param exchange 
     */
    public AMQPProducer(String virtualHost, String amqpHost, String exchange) {
        this.host = amqpHost;
        this.virtualHost = virtualHost;
        this.exchange = exchange;
    }


    public void sendMessage(ChronMessage2 message, String bindingKey) throws IOException {
        if ( null == message ) {
            throw new IllegalArgumentException("Cannot have null message");
        }
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        if ( null != virtualHost) {
            factory.setVirtualHost(virtualHost);
        }

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        channel.exchangeDeclare(exchange, exchangeType, true);
        byte[] amqpMessage = message.createMessage();
        BasicProperties props = new BasicProperties.Builder()
                                        .priority(0)
                                        .deliveryMode(deliveryMode)
                                        .headers(message.getHeader())
                                        .build();
                                        
       channel.basicPublish(exchange, bindingKey, props, amqpMessage);
       channel.close();
       connection.close();
    }
     
}
