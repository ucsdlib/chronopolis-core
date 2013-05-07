/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.chronopolis.replicate;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.collection.CollectionInitMessage;
import org.json.simple.JSONObject;

/**
 *
 * @author shake
 */
public class AMQPCollectionConsumer {
    private String EXCHANGE;
    public void consume() throws IOException, InterruptedException, ClassNotFoundException{
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE, "topic");
        String queueName = channel.queueDeclare().getQueue();

        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, consumer);

        while ( true ) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();

            // Create an object from the byte stream
            ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
            ChronBody body = (ChronBody)ois.readObject();

            //manifestURL = obj.remove("manifest"); 
            CollectionInitMessage cim = new CollectionInitMessage();
            cim.setHeader(delivery.getProperties().getHeaders());
            cim.setBody(cim.getType(), body);

            // Register collection with ACE
            // POST obj to localhost:8080/ace-am/rest/collection

            // Load token store (File xfer? Yea probably the best)
        }
    }

}
