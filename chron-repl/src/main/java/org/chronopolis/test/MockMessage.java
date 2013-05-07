/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.test;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import java.io.IOException;
import org.chronopolis.messaging.MessageBuilder;
import org.chronopolis.messaging.MessageType;
import org.chronopolis.messaging.file.FileTransferMessage;

/**
 *
 * @author shake
 */
public class MockMessage {
    private static String DEPOSITOR = "depositor";
    private static String COLLECTION = "collection";
    private static String TOKENSTORE = "tokenStore";
    private static String AUDITPERIOD = "audit.period";
    private static final String EXCHANGE = "chronopolis-control";
    private static final String VHOST = "chronopolis";
    private static final String bindingKey = "chron.mock";
    // 2 == Persistent
    private static final Integer deliveryMode = 2;
    private static final Integer priority = 0;
    private boolean RUN = true;
    
    public void testMessage( ) throws IOException {
        MessageBuilder mb = new MessageBuilder();
        mb.setMessageName(MessageType.DISTRIBUTE_COLL_INIT);
		//MessageType.O_DISTRIBUTE_TRANSFER_REQUEST;
        
        mb.set(DEPOSITOR, "test_deposit");
        mb.set(COLLECTION, "test_collection");
        mb.set(TOKENSTORE, "tokensTore");
        mb.set(AUDITPERIOD, "132");
        
        FileTransferMessage ftm = new FileTransferMessage(MessageType.DISTRIBUTE_TRANSFER_REQUEST);
        ftm.setDepositor("test_deposit");
        ftm.setDigest("test-digest");
        ftm.setDigestType("SHA-256");
        ftm.setFilename("a/really/real/file.txt");
        ftm.setSource("localhost");
        ftm.setReturnKey("R-ACK-WHATEVER");
        ftm.setLocation("https://sometomcatinstance/chron-ingest/12912743");
       
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("adapt-mq.umiacs.umd.edu");
        factory.setVirtualHost(VHOST);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE, "topic", true);

		byte[] message = ftm.createMessage();
		BasicProperties props = new BasicProperties
									.Builder()
                                    .priority(priority)
                                    .deliveryMode(deliveryMode)
									.headers(ftm.getHeader())
									.build();

        channel.basicPublish(EXCHANGE, bindingKey, 
                             props, message);
        System.out.println("Waiting on ack?");
		channel.close();
		connection.close();
        System.out.println("Probably not");

    }
    
}
