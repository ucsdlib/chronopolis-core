/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Totally based off of Andrew's Producer for DPN
 *
 * @author shake
 */
public class IntakeProducer {

    private ChronProducer producer;

    public IntakeProducer(ChronProducer producer) {
        this.producer = producer;
    }
    
    private enum PRODUCER_OPTION {
        SEND_INTAKE_REQUEST, QUIT, UNKNOWN;
        
        private static PRODUCER_OPTION fromString(String text) {
            switch (text) {
                case "S":
                    return SEND_INTAKE_REQUEST;
                case "Q":
                    return QUIT;
                default:
                    return UNKNOWN;
            }
        }
    }

    public void sendIntakeRequest() throws IOException {
        String vhost = "chronopolis";
        String exchange = "chronopolis-control";
        String routingKey = "ingest.package-ready.umiacs";
        //AMQPProducer producer = new AMQPProducer("chronopolis", "adapt-mq.umiacs.umd.edu", "chronopolis-exchange");
        
        PackageReadyMessage msg = new PackageReadyMessage(); 
        msg.setDepositor("chronopolis");
        msg.setLocation("https://chron-stage.umiacs.umd.edu/package-name");
        msg.setPackageName("package-name");
        //msg.setProtocol("https");
        msg.setSize(1024);
        msg.getHeader();
        msg.getChronHeader().setOrigin("umiacs"); // t(*-*t)
        msg.getChronHeader().setReturnKey("chron.ingest.package-reply");

        
        System.out.println("Sending " + msg.toString());
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("adapt-mq.umiacs.umd.edu");
        factory.setVirtualHost(vhost);

        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(exchange, "topic", true);

		byte[] message = msg.createMessage();
        /*
		BasicProperties props = new BasicProperties
									.Builder()
									.headers(msg.getHeader())
									.build();
                                    */
        channel.basicPublish(exchange, routingKey, 
                             null, message);
        System.out.println("Waiting on ack?");
		channel.close();
		connection.close();
        System.out.println("Probably not");
    }
    
    public void run() {
        boolean done = false;
        while (!done) {
            PRODUCER_OPTION option = inputOption();
            
            if ( option.equals(PRODUCER_OPTION.SEND_INTAKE_REQUEST)) {
                PackageReadyMessage msg = MessageFactory.DefaultPackageReadyMessage();
                producer.send(msg,"package.ingest.broadcast"); 
                /*
                try {
                    sendIntakeRequest();
                } catch (IOException ex) {
                    Logger.getLogger(IntakeProducer.class.getName()).log(Level.SEVERE, null, ex);
                }
                */
            } else if (option.equals(PRODUCER_OPTION.QUIT)) {
                done = true;
            } else {
                System.out.println("Unknown?");
            }
        }
    }
    
    private PRODUCER_OPTION inputOption() {
        PRODUCER_OPTION option = PRODUCER_OPTION.UNKNOWN;
        while ( option.equals(PRODUCER_OPTION.UNKNOWN)) {
            StringBuilder sb = new StringBuilder("Enter Option: ");
            String sep = " | ";
            for (PRODUCER_OPTION value : PRODUCER_OPTION.values()) {
                if (!value.equals(PRODUCER_OPTION.UNKNOWN)) {
                    sb.append(value.name());
                    sb.append(" [");
                    sb.append(value.name().charAt(0));
                    sb.append("]");
                    sb.append(sep);
                }
            }
            sb.replace(sb.length()-sep.length(), sb.length(), " -> "); //The one difference, mwahhaha
            System.out.println(sb.toString());
            option = PRODUCER_OPTION.fromString(readLine().trim());
        }
        return option;
    }
    
    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read STDIN");
        }
    }
    
    public static void main(String [] args) {
        System.out.println("Hello wrld");
    
        GenericXmlApplicationContext text = new GenericXmlApplicationContext(
                "classpath:/rabbit-context.xml");

        ChronProducer p = (ChronProducer) text.getBean("producer");

        
        IntakeProducer producer = new IntakeProducer(p);
        producer.run();
        
        System.out.println("Shutting down, shutting shutting down");
    }
}
