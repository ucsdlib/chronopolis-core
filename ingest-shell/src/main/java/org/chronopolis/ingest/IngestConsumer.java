/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.ingest;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.ingest.config.IngestConfiguration;
import org.chronopolis.ingest.config.IngestJPAConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;


/**
 *
 * @author shake
 */
public final class IngestConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(IngestConsumer.class);

    private IngestConsumer() {
    }

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

        while (!done) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                LOG.error("Interrupted {}", ex);
            }

            System.out.println("Enter 'q' to exit: ");
            if ("q".equalsIgnoreCase(readLine())) {
                LOG.info("Shutting down");
                done = true;
            }
        }

        context.close();
    }

}

