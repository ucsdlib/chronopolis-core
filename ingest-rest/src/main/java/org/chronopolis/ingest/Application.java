package org.chronopolis.ingest;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.chronopolis.ingest.api.StagingController;
import org.chronopolis.ingest.model.Bag;
import org.chronopolis.ingest.model.Node;
import org.chronopolis.ingest.model.ReplicationAction;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.orm.jpa.EntityScan;
import org.springframework.context.annotation.ComponentScan;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Random;

/**
 * Created by shake on 11/6/14.
 */
@ComponentScan(basePackageClasses = {
        IngestSettings.class,
        StagingController.class
})
@EntityScan(basePackages = "org.chronopolis.ingest.model")
@EnableAutoConfiguration
public class Application implements CommandLineRunner {
    private final Logger log = LoggerFactory.getLogger(Application.class);

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    IngestSettings ingestSettings;

    public static void main(String[] args) {
        SpringApplication.exit(SpringApplication.run(Application.class));
    }

    @Override
    public void run(final String... args) throws Exception {
        System.out.println("Creating nodes...");
        List<Node> nodeList = Lists.newArrayList();
        for (String s : Sets.newHashSet("umiacs", "sdsc", "ncar", "ucsd")) {
            Node n = new Node(s, s);
            nodeRepository.save(n);
            nodeList.add(n);
        }

        System.out.println("Creating bags...");
        Random r = new Random();
        List<Bag> bagList = Lists.newArrayList();
        for (int i = 0; i < 100; i++) {
            Bag b = new Bag();
            b.setName("bag-" + i);
            b.setDepositor("test-depositor");
            b.setFixityAlgorithm("SHA-256");
            b.setLocation("chrono@chronopolis-stage:/export/bags/test-bag-" + i);
            b.setProtocol("rsync");
            b.setSize(r.nextInt(50000));
            b.setTagManifestDigest("");
            b.setTokenDigest("");
            b.setTokenLocation("chrono@chronopolis-stage:/export/tokens/test-bag-" + i + "-tokens");
            bagRepository.save(b);
            bagList.add(b);
        }

        System.out.println("Creating transfers...");
        for (Bag b : bagList) {
            // create xfer object for each node
            for (Node n : nodeList) {
                ReplicationAction action = new ReplicationAction(n,
                        b.getId(),
                        b.getTagManifestDigest(),
                        b.getTokenDigest(),
                        "",
                        "");
                replicationRepository.save(action);
            }
        }

        Object[] values = new Object[]{ingestSettings.getNode(), ingestSettings.getBagStage(), ingestSettings.getTokenStage()};
        log.info("Autowired properties with settings: {}", values);
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
