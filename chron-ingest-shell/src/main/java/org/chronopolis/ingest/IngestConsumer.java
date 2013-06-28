/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 *
 * @author shake
 */
public class IngestConsumer {

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }


    /*
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
            msg.setBody(body);
            msg.setHeader(header.getHeader());

            System.out.println("Recieved message\nHeaders { " + header.toString() + " }"
                    + "\nBody { " + msg.toString() + " }");
            
            channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
        } 
    }
    */
    
    public static void main(String [] args) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/rabbit-context.xml");
        boolean done = false;
        while (!done) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }
            System.out.println("Enter 'q' to exti: ");
            if ("q".equalsIgnoreCase(readLine())) {
                System.out.println("Shutdting dinow");
                done = true;
            }
        }

        context.close();
        System.out.println("Closed for business");
    }
}
