package org.chronopolis.ingest.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.chronopolis.rest.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Random;

/**
 * Created by shake on 1/8/15.
 */
@Configuration
@Profile("development")
public class DevConfig {

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    RestoreRepository restoreRepository;

    @Autowired
    IngestSettings ingestSettings;

    @Bean
    public boolean createData() {
        System.out.println("Creating nodes...");
        List<Node> nodeList = Lists.newArrayList();
        for (String s : Sets.newHashSet("umiacs", "sdsc", "ncar", "ucsd")) {
            Node n = nodeRepository.findByUsername(s);
            if (n == null) {
                n = new Node(s, s);
            }
            nodeRepository.save(n);
            nodeList.add(n);
        }

        System.out.println("Creating bags...");
        Random r = new Random();
        List<Bag> bagList = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            Bag b = new Bag("bag-" + i, "test-depositor");
            b.setFixityAlgorithm("SHA-256");
            b.setLocation("bags/test-bag-" + i);
            b.setSize(r.nextInt(50000));
            b.setTagManifestDigest("");
            b.setTokenDigest("");
            b.setTokenLocation("tokens/test-bag-" + i + "-tokens");
            bagRepository.save(b);
            bagList.add(b);
        }

        System.out.println("Creating transfers and restorations...");
        Random ran = new Random();
        for (Bag b : bagList) {
            // create xfer object for each node
            for (Node n : nodeList) {
                Replication action = new Replication(n,
                        b,
                        ingestSettings.getBagStage() + "/" + b.getLocation() ,
                        ingestSettings.getTokenStage() + "/" + b.getTokenLocation());

                if (ran.nextInt(100) < 10) {
                    action.setStatus(ReplicationStatus.STARTED);
                }

                replicationRepository.save(action);
            }

            Restoration restoration = new Restoration(b.getDepositor(),
                    b.getName(),
                    ingestSettings.getRestore() + "/" + b.getLocation());
            restoreRepository.save(restoration);

        }

        return true;
    }

}
