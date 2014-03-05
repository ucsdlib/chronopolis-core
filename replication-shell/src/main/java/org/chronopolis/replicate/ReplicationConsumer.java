package org.chronopolis.replicate;

import org.chronopolis.amqp.ChronProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by shake on 2/12/14.
 */
public class ReplicationConsumer {
    private static final Logger log = LoggerFactory.getLogger(ReplicationConsumer.class);
    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Can't read from STDIN");
        }
    }

    public static void main(String [] args) {
        String aceRegister = "aceRegister";
        String aceCheck = "aceCheck";

        String aceRegisterVal = System.getProperty(aceRegister);
        String aceCheckVal = System.getProperty(aceCheck);


        if (aceRegisterVal != null) {
            //props.setAceRegister(Boolean.parseBoolean(aceRegisterVal));
            log.info("Captured aceRegister: '{}'", Boolean.parseBoolean(aceRegisterVal));
        }

        if (aceCheckVal != null) {
            //props.setAceCheck(Boolean.parseBoolean(aceCheckVal));
            log.info("Capture aceCheck: '{}'", Boolean.parseBoolean(aceCheckVal));
        }

        GenericXmlApplicationContext context = new GenericXmlApplicationContext(
                "classpath:/application-context.xml");
        boolean done = false;
        ChronProducer p = (ChronProducer) context.getBean("producer");
        ReplicationProperties props = (ReplicationProperties) context.getBean("properties");

        while (!done) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }

            System.out.println("Enter 'q' to exit: ");
            if ("q".equalsIgnoreCase(readLine())) {
                System.out.println("Shutting down");
                done = true;
            }
        }

        context.close();
    }
}
