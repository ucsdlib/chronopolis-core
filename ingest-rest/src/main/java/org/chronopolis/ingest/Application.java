package org.chronopolis.ingest;

import org.chronopolis.ingest.api.StagingController;
import org.chronopolis.ingest.model.Bag;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Random;

/**
 * Created by shake on 11/6/14.
 */
@ComponentScan(basePackages = "org.chronopolis.ingest.api")
@EnableAutoConfiguration
public class Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Application.class));
    }

    @Override
    public void run(final String... args) throws Exception {
        System.out.println("Creating test data...");
        Random r = new Random();
        for (int i = 0; i < 100; i++) {
            Bag b = new Bag();
            b.setName("bag-" + i);
            b.setDepositor("test-depositor");
            b.setFixityAlgorithm("SHA-256");
            b.setLocation("chrono@chronopolis-stage:/export/bags/test-bag-" + i);
            b.setProtocol("rsync");
            b.setSize(r.nextInt(50000));
            b.setTagManifestDigest("");
            b.setTokenDigset("");
            b.setTokenLocation("chrono@chronopolis-stage:/export/tokens/test-bag-" + i + "-tokens");
            StagingController.bags.add(b);
        }

        boolean done = false;
        System.out.println("Enter 'q' to quit");
        while (!done) {
            if ("q".equalsIgnoreCase(readLine())) {
                done = true;
            }
        }
    }

    private static String readLine() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            return reader.readLine();
        } catch (IOException ex) {
            throw new RuntimeException("Unable to read STDIN");
        }
    }


}
