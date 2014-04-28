/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.ingest.config.IngestConfiguration;
import org.chronopolis.ingest.config.IngestJPAConfiguration;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


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
    
    public static void main(String [] args) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(IngestJPAConfiguration.class);
        context.register(IngestConfiguration.class);
        context.refresh();

        boolean done = false;
        ChronProducer p = (ChronProducer) context.getBean("producer");
        IngestProperties props = (IngestProperties) context.getBean(IngestProperties.class);

        Queue bQueue = (Queue) context.getBean("broadcastQueue");

        System.out.println(bQueue.getName());

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
