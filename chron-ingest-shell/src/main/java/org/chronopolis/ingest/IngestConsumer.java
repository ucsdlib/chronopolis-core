/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chronopolis.messaging.base.ChronBody;
import org.chronopolis.messaging.base.ChronHeader;
import org.chronopolis.messaging.pkg.PackageReadyMessage;

/**
 *
 * @author shake
 */
public class IngestConsumer {

    public void consume() throws IOException, InterruptedException, ClassNotFoundException {
        String vhost = "chronopolis";
        String exchange = "chronopolis-control";
        String routingKey = "ingest.package-ready.umiacs";
        boolean consume = true;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("adapt-mq.umiacs.umd.edu");
        factory.setVirtualHost(vhost);

        Connection connection = factory.newConnection();
        connection.addShutdownListener(new ShutdownListener() {
            @Override
            public void shutdownCompleted(ShutdownSignalException sse) {
                if (sse.isHardError()) {
                    System.out.println("Hard shutdown occured " + sse.getMessage());
                } else if (sse.isInitiatedByApplication()) {
                    System.out.println("Initiated by app " + sse.getMessage());
                } else {
                    System.out.println("Normal shutdown? " + sse.getMessage());
                }
            }
        });

        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchange, "topic", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, exchange, routingKey);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, consumer);

        System.out.println("Waiting to recieve messages. Press ctrl+c to exit");
        while (consume) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery(1500);
            System.out.println("yo");

            // Check for timeout
            if ( null == delivery ) {
                continue;
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);

            Object o = ois.readObject();

//            if ( !(ois.readObject() instanceof PackageReadyMessage)) {
  //              throw new RuntimeException("Error");
    //        }
            System.out.println(o.getClass().getName());
            System.out.println(delivery.getProperties().getHeaders());
            if ( !(o instanceof ChronBody)) {
                throw new RuntimeException("Error");
            }
            
			ChronBody body = (ChronBody) o;
            ChronHeader header = new ChronHeader(delivery.getProperties().getHeaders());
            PackageReadyMessage msg = new PackageReadyMessage();
            msg.setBody(msg.getType(), body);
            msg.setHeader(header.getHeader());

            System.out.println("Recieved message\nHeaders { " + header.toString() + " }"
                    + "\nBody { " + msg.toString() + " }");
            
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } 
    }
    
    public static void main(String [] args) {
        IngestConsumer consumer = new IngestConsumer();
        try {
            consumer.consume();
        } catch (IOException | InterruptedException | ClassNotFoundException ex) {
            Logger.getLogger(IngestConsumer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
