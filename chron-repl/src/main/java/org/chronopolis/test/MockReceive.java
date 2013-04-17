/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.test;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownListener;
import com.rabbitmq.client.ShutdownSignalException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.NoSuchAlgorithmException;
import org.chronopolis.base.message.ChronBody;
import org.chronopolis.base.message.ChronHeader;
import org.chronopolis.base.message.FileTransferMessage;
import org.chronopolis.messaging.ChronMessage;
import org.chronopolis.messaging.MessageType;

/**
 *
 * @author shake
 */
public class MockReceive extends Thread {
    private static final String EXCHANGE = "chronopolis-control";
    private static final String VHOST = "chronopolis";
    private static final String bindingKey = "chron.mock";
    private boolean RUN = true;

	public MockReceive() {
		System.out.println("Instantiated");
	}
	
	@Override
	public void run() {
		try {
			consume();
		} catch (IOException ex) {
			//Logger.getLogger(MockReceive.class.getName()).log(Level.SEVERE, null, ex);
		} catch (InterruptedException ex) {
			//Logger.getLogger(MockReceive.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ClassNotFoundException ex) {
			//Logger.getLogger(MockReceive.class.getName()).log(Level.SEVERE, null, ex);
		} catch (NoSuchAlgorithmException ex) {
			//Logger.getLogger(MockReceive.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public void setRun(boolean run) {
		this.RUN = run;
	}

    public void consume() throws IOException, InterruptedException, ClassNotFoundException, NoSuchAlgorithmException {
		System.out.println("Starting consume");
        MessageType type = MessageType.O_DISTRIBUTE_TRANSFER_REQUEST;
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("adapt-mq.umiacs.umd.edu");
        factory.setVirtualHost(VHOST);
        Connection connection = factory.newConnection();
        connection.addShutdownListener(new ShutdownListener() {
            public void shutdownCompleted(ShutdownSignalException sse) {
                if ( sse.isHardError() ) {
//                    LOG.fatal("Fatal Exception shutting down: " + sse.getReason());
                }else if (sse.isInitiatedByApplication()) {
 //                   LOG.error("Application Exception: " + sse.getReason());
                }

  //              LOG.info("Shutting down or something" + sse.getReason());
            }

        });
        Channel channel = connection.createChannel();

		System.out.println("Binding to chron.mock");
        // Make our exchange if necessary and bind our queue 
        channel.exchangeDeclare(EXCHANGE, "topic", true);
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE, bindingKey);

        QueueingConsumer consumer = new QueueingConsumer(channel);
        boolean autoAck = false;
        channel.basicConsume(queueName, autoAck, consumer);


        while ( RUN ) {
            QueueingConsumer.Delivery delivery = consumer.nextDelivery(1000);
            // check for timeout
            if (delivery == null) {
                continue;
            }

            System.out.println("Got delivery");
            // Create an object from the byte stream
            ByteArrayInputStream bais = new ByteArrayInputStream(delivery.getBody());
            ObjectInputStream ois = new ObjectInputStream(bais);
			ChronBody body = (ChronBody) ois.readObject();
            ChronHeader header = new ChronHeader(delivery.getProperties().getHeaders());

			FileTransferMessage message = new FileTransferMessage(type,
                                                                  header, 
                                                                  body);

            for (String k: body.getBody().keySet()) {
                System.out.println(k + " : " + body.getBody().get(k));
            }
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        }
        channel.close();
        connection.close();

		System.out.println("Ending mock recieve");
    }
}
