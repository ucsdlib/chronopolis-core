/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.intake;

import gov.loc.repository.bagit.Bag;
import gov.loc.repository.bagit.BagFactory;
import gov.loc.repository.bagit.PreBag;
import gov.loc.repository.bagit.writer.impl.FileSystemWriter;
import org.chronopolis.common.settings.ChronopolisSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.IngestRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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

    private ChronopolisSettings settings;
    private IngestAPI ingestAPI;

    @Autowired
    public IntakeProducer(ChronopolisSettings settings,
                          IngestAPI ingestAPI) {
        this.settings = settings;
        this.ingestAPI = ingestAPI;
    }

    private enum PRODUCER_OPTION {
        SEND_STATIC_INTAKE_REQUEST, CREATE_INTAKE_REQUEST, RESTORE_REQUEST, DIRECTORY_SCAN, T_REST, BAG_IT, QUIT, UNKNOWN;

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
                    return T_REST;
                case "B":
                    return BAG_IT;
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

            if (option.equals(PRODUCER_OPTION.QUIT)) {
                done = true;
            } else if (option.equals(PRODUCER_OPTION.DIRECTORY_SCAN)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Directory: ");
                directory = readLine();

                scanDirectory(depositor, directory);
            } else if (option.equals(PRODUCER_OPTION.T_REST)) {
                System.out.print("Depositor: ");
                depositor = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();

                sendRest(depositor, bagName);
            } else if (option.equals(PRODUCER_OPTION.BAG_IT)) {
                System.out.print("Bag Directory: ");
                directory = readLine();
                System.out.print("Bag Name: ");
                bagName = readLine();
                bagIt(directory, bagName);
            } else {
                System.out.println("Unknown?");
            }
        }
    }

    private void sendRest(final String depositor, final String bagName) {
        String path = depositor + "/" + bagName;
        // TODO: Remove magic values...
        List<String> replicatingNodes = new ArrayList<>();
        replicatingNodes.add("ncar");
        replicatingNodes.add("ucsd");
        replicatingNodes.add("umiacs");
        IngestRequest request = new IngestRequest();
        request.setDepositor(depositor);
        request.setLocation(path);
        request.setName(bagName);
        request.setReplicatingNodes(replicatingNodes);

        ingestAPI.stageBag(request);
    }

    /**
     * Scan a directory and send each bag underneath it
     * Should be of the form
     * /stage/directory/
     * | bag_1/
     * | bag_2/
     * ...
     * | bag_n/
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
                // sendMessage(depositor, directory + "/" + f, f);
            }
        }

    }


    /**
     * Create a bag under /stage/bagName
     * Should be of the form
     */
    private void bagIt(final String directory, final String bagName) {

        // get the path for a bag
        Path bagPath = Paths.get(directory);
        if (!bagPath.toFile().exists()) {
            System.out.println("ERROR: The directory " + directory + " does exist.");
            return;
        }

        // get the bag destination folder
        String bagStage = settings.getBagStage();
        File bagDir = new File(bagStage + "/" + bagName);
        if (bagDir.exists()) {
            System.out.println("ERROR: The bag " + bagStage + "/" + bagName + " exists.");
            return;
        }

        BagFactory bf = new BagFactory();
        PreBag pb = bf.createPreBag(bagPath.toFile());
        pb.makeBagInPlace(BagFactory.Version.V0_97, false);
        Bag b = bf.createBag(bagPath.toFile(), BagFactory.LoadOption.BY_FILES);

        //adding some optional metadata: 
        Date d = new Date();
        b.getBagInfoTxt().putList("createDate", d.toString());
        b.getBagInfoTxt().putList("modified", d.toString());
        b.getBagInfoTxt().putList("numberOfModifications", Integer.toString(1));

        b.makeComplete();
        FileSystemWriter fsw = new FileSystemWriter(bf);
        fsw.write(b, bagDir);
        System.out.println("The bag " + bagStage + "/" + bagName + " is created.");

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

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(IntakeProducer.class, args));
    }
}
