/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.messaging.logger;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ConsumerCancelledException;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.base.ChronMessage2;
import org.chronopolis.messaging.logger.processor.GenericMessageProcessor;

/**
 *
 * @author shake
 */
public class AMQPLogger implements Runnable {
    private String key;
    private String virtualHost;
    private String host;
    private String exchange;
    private boolean consume = true;
    private long timeout = 3600;
    private GenericMessageProcessor processor = new GenericMessageProcessor();

    public AMQPLogger(String key, String host, String virtualHost, String exchange) {
        this.key = key;
        this.host = host;
        this.virtualHost = virtualHost;
        this.exchange = exchange;
    }

    public AMQPLogger(String key, String host, String exchange) { 
        this.key = key;
        this.host = host;
        this.exchange = exchange;
        this.virtualHost = "";
    }

    /*
     * TODO: Most of this is really generic set up/get delivery and make it a 
     * ChronMessage. Should work to migrate this (and the ingest consumer) to the
     * AMQPConsumer which will consume based on a routingKey/host/etc and fire off
     * a processor for the message type (maybe map MessageType -> Processor in the
     * class). 
     * 
     * OR
     * 
     * Let's move to the SpringAMQP Libs which will allow us to define an interface
     * which consumes messages and takes care of the setup/teardown/building message objects.
     * 
     */
    public void run() { 
        // Set up connection
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(host);
        if (!virtualHost.isEmpty()) {
            factory.setVirtualHost(virtualHost);
        }
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(exchange, "topic", true);
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, exchange, key);

            QueueingConsumer consumer = new QueueingConsumer(channel);
            boolean autoAck = false;
            channel.basicConsume(queueName, autoAck, consumer);

            while ( consume ) {
                QueueingConsumer.Delivery delivery = consumer.nextDelivery(timeout);

                // Check for timeout
                if ( delivery == null) {
                    continue;
                }

                ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
                ObjectInputStream ois = new ObjectInputStream(bais);

                Object o = ois.readObject();

                ChronBody body = (ChronBody) o;
                ChronHeader header = new ChronHeader(delivery.getProperties().getHeaders());
                ChronMessage2 msg = new ChronMessage2(body.getType());
                msg.setBody(body);
                msg.setHeader(header.getHeader());
                processor.process(msg);
            }
        } catch (IOException ex) {
            Logger.getLogger(AMQPLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(AMQPLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ShutdownSignalException ex) {
            Logger.getLogger(AMQPLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ConsumerCancelledException ex) {
            Logger.getLogger(AMQPLogger.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(AMQPLogger.class.getName()).log(Level.SEVERE, null, ex);
        }
        
       

    }



    
}
