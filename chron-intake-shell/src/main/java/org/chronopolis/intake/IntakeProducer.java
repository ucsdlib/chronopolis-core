/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.springframework.amqp.core.Message;
import org.springframework.context.support.GenericXmlApplicationContext;

/**
 * Totally based off of Andrew's Producer for DPN
 * TODO: This is blocking when trying to quit... figure out why... prime culprit is the producer
 *
 * @author shake
 */
public class IntakeProducer {
    
    private ChronProducer producer;
    private GenericProperties props;
    private MessageFactory messageFactory;
    
    public IntakeProducer(ChronProducer producer, MessageFactory messageFactory) {
        this.producer = producer;
        this.messageFactory = messageFactory;
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
    
    public void run() {
        boolean done = false;
        while (!done) {
            PRODUCER_OPTION option = inputOption();
            
            if ( option.equals(PRODUCER_OPTION.SEND_INTAKE_REQUEST)) {
                PackageReadyMessage msg = messageFactory.packageReadyMessage("chron",
                        Digest.SHA_256,
                        "myDPNBag",
                        "test-dpn", 400);
                producer.send(msg,"package.ingest.broadcast");
            } else if (option.equals(PRODUCER_OPTION.QUIT)) {
                done = true;
            } else {
                System.out.println("Unknown?");
            }
        }
        System.out.println("Leaving");
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
        MessageFactory factory = (MessageFactory) text.getBean("messageFactory");
        
        
        IntakeProducer producer = new IntakeProducer(p, factory);
        producer.run();
        
        System.out.println("Shutting down, shutting shutting down");
    }
}
