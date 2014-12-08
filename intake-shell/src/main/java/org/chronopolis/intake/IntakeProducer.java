/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.common.digest.Digest;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.chronopolis.messaging.pkg.PackageReadyMessage;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.IngestRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;

/**
 * Totally based off of Andrew's Producer for DPN
 *
 * @author shake
 */
@Component
@ComponentScan(basePackages = {
        "org.chronopolis.common.settings",
        "org.chronopolis.intake.config"
})
@EnableAutoConfiguration
public class IntakeProducer implements CommandLineRunner {

    private ChronProducer producer;
    private ChronopolisSettings settings;
    private MessageFactory messageFactory;
    private IngestAPI ingestAPI;

    @Autowired
    public IntakeProducer(ChronProducer producer,
                          MessageFactory messageFactory,
                          ChronopolisSettings settings,
                          IngestAPI ingestAPI) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.settings = settings;
        this.ingestAPI = ingestAPI;
    }

    private enum PRODUCER_OPTION {
        SEND_STATIC_INTAKE_REQUEST, CREATE_INTAKE_REQUEST, RESTORE_REQUEST, DIRECTORY_SCAN, REST, QUIT, UNKNOWN;

        private static PRODUCER_OPTION fromString(String text) {
            switch (text) {
                case "C":
                    return CREATE_INTAKE_REQUEST;
                case "S":
                    return SEND_STATIC_INTAKE_REQUEST;
                case "R":
                    return RESTORE_REQUEST;
                case "D":
                    return DIRECTORY_SCAN;
                case "T":
                    return REST;
                case "Q":
                case "q":
                    return QUIT;
                default:
                    return UNKNOWN;
            }
        }
    }

    @Override
    public void run(final String... strings) throws Exception {
        boolean done = false;
        while (!done) {
            PRODUCER_OPTION option = inputOption();
            String depositor, bagName, directory;

            if (option.equals(PRODUCER_OPTION.SEND_STATIC_INTAKE_REQUEST)) {
                sendMessage("umiacs", "myDPNBag", "myDPNBag");
            } else if (option.equals(PRODUCER_OPTION.CREATE_INTAKE_REQUEST)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();

                sendMessage(depositor, bagName, bagName);
            } else if (option.equals(PRODUCER_OPTION.RESTORE_REQUEST)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();

                sendRestore(depositor, bagName);
             } else if (option.equals(PRODUCER_OPTION.QUIT)) {
                done = true;
            } else if (option.equals(PRODUCER_OPTION.DIRECTORY_SCAN)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Directory: ");
                directory = readLine();

                scanDirectory(depositor, directory);
            } else if (option.equals(PRODUCER_OPTION.REST)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();

                sendRest(depositor, bagName);

            } else {
                System.out.println("Unknown?");
            }
        }
    }

    private void sendRest(final String depositor, final String bagName) {
        String path = depositor + "/" + bagName;
        IngestRequest request = new IngestRequest();
        request.setDepositor(depositor);
        request.setLocation(path);
        request.setName(bagName);
        ingestAPI.putBag(request);
    }

    /**
     * Scan a directory and send each bag underneath it
     * Should be of the form
     * /stage/directory/
     *            | bag_1/
     *            | bag_2/
     *            ...
     *            | bag_n/
     *
     * @param depositor
     * @param directory
     */
    private void scanDirectory(final String depositor, final String directory) {
        Path toScan = Paths.get(settings.getBagStage(), directory);
        for (String f : toScan.toFile().list()) {
            Path bag = toScan.resolve(f);
            if (bag.toFile().isDirectory()) {
                System.out.printf("Sending %s %s %s\n", depositor, directory + "/" + f, f);
                sendMessage(depositor, directory + "/" + f, f);
            }
        }


    }

    /**
     * Send a restore request message for a bag in chronopolis
     *
     * @param depositor
     * @param bagName
     */
    private void sendRestore(final String depositor, final String bagName) {
        Path location = Paths.get(settings.getRestore(), UUID.randomUUID().toString());
        ChronMessage restore = messageFactory.collectionRestoreRequestMessage(bagName, depositor, location.toString());
        producer.send(restore, RoutingKey.INGEST_BROADCAST.asRoute());
    }

    private void sendMessage(String depositor, String location, String bagName) {
        Path collectionPath = Paths.get(settings.getBagStage(), location);

        // Calculate the bag size for our message
        final int [] bagSize = {0};
        try {
            Files.walkFileTree(collectionPath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
                    bagSize[0] += basicFileAttributes.size();
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            // We don't really care about errors here, at least not yet
            System.out.println("Could not calculate bag size");
            bagSize[0] = 0;
        }

        PackageReadyMessage msg = messageFactory.packageReadyMessage(
                depositor,
                Digest.SHA_256,
                location,
                bagName,
                bagSize[0]
        );

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
        SpringApplication.exit(SpringApplication.run(IntakeProducer.class, args));
    }
}
