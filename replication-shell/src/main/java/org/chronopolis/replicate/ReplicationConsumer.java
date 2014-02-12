package org.chronopolis.replicate;

import org.chronopolis.amqp.ChronProducer;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by shake on 2/12/14.
 */
public class ReplicationConsumer {
    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String [] args) {
        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/application-context.xml");
        boolean done = false;
        ChronProducer p = (ChronProducer) context.getBean("producer");
        ReplicationProperties props = (ReplicationProperties) context.getBean("properties");

        while (!done) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            System.out.println("Enter 'q' to exit: ");
            if ("q".equalsIgnoreCase(readLine())) {
                System.out.println("Shutting down");
                done = true;
            }
        }

        context.close();
        System.out.println("Closed for business");
    }
}
