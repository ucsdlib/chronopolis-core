/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.properties.GenericProperties;
import org.chronopolis.intake.config.IntakeConfig;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Totally based off of Andrew's Producer for DPN
 *
 * @author shake
 */
public class IntakeProducer {

    private ChronProducer producer;
    private GenericProperties props;
    private MessageFactory messageFactory;

    public IntakeProducer(ChronProducer producer, MessageFactory messageFactory, GenericProperties props) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.props = props;
    }

    private enum PRODUCER_OPTION {
        SEND_STATIC_INTAKE_REQUEST, CREATE_INTAKE_REQUEST, PUSH_STATIC_INTAKE_TO_DPN, QUIT, UNKNOWN;

        private static PRODUCER_OPTION fromString(String text) {
            switch (text) {
                case "C":
                    return CREATE_INTAKE_REQUEST;
                case "S":
                    return SEND_STATIC_INTAKE_REQUEST;
                case "P":
                    return PUSH_STATIC_INTAKE_TO_DPN;
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

            if (option.equals(PRODUCER_OPTION.SEND_STATIC_INTAKE_REQUEST)) {
                sendMessage("umiacs", "myDPNBag", "myDPNBag", false);
            } else if (option.equals(PRODUCER_OPTION.PUSH_STATIC_INTAKE_TO_DPN)) {
                sendMessage("umiacs", "myDPNBag", "myDPNBag", true);
            } else if (option.equals(PRODUCER_OPTION.CREATE_INTAKE_REQUEST)) {
                String depositor, bagName;
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();

                sendMessage(depositor, bagName, bagName, false);
            } else if (option.equals(PRODUCER_OPTION.QUIT)) {
                done = true;
            } else {
                System.out.println("Unknown?");
            }
        }
        System.out.println("Leaving");
    }

    private void sendMessage(String depositor, String location, String bagName, boolean toDPN) {
        System.out.println(props == null);
        System.out.println(props.getStage() == null);
        Path collectionPath = Paths.get(props.getStage(), location);
        final int [] bagSize = {0};
        try {
            Files.walkFileTree(collectionPath, new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    bagSize[0] += basicFileAttributes.size();
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path path, IOException e) throws IOException {
                    return FileVisitResult.TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path path, IOException e) throws IOException {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            System.out.println("Could not calculate bag size");
            bagSize[0] = 0;
        }

        PackageReadyMessage msg = messageFactory.packageReadyMessage(
                depositor,
                Digest.SHA_256,
                location,
                bagName,
                bagSize[0],
                toDPN
        );

        System.out.println(msg.toString());
        producer.send(msg, RoutingKey.INGEST_BROADCAST.asRoute());
    }

    private PRODUCER_OPTION inputOption() {
        PRODUCER_OPTION option = PRODUCER_OPTION.UNKNOWN;
        while (option.equals(PRODUCER_OPTION.UNKNOWN)) {
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

            //The one difference, mwahhaha
            sb.replace(sb.length() - sep.length(), sb.length(), " -> ");
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
        GenericXmlApplicationContext text = new GenericXmlApplicationContext(
                "classpath:/rabbit-context.xml");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(IntakeConfig.class);
        context.refresh();

        ChronProducer p = (ChronProducer) text.getBean("producer");
        MessageFactory factory = (MessageFactory) text.getBean("messageFactory");
        GenericProperties properties = text.getBean(GenericProperties.class);


        IntakeProducer producer = new IntakeProducer(p, factory, properties);
        producer.run();

        text.close();
        context.close();

        System.out.println("Shutting down, shutting shutting down");
    }
}
