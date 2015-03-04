package org.chronopolis.ingest.config;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.chronopolis.ingest.repository.TokenRepository;
import org.chronopolis.ingest.task.TokenThreadPoolExecutor;
import org.chronopolis.rest.models.AceToken;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.Restoration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

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
    TokenRepository tokenRepository;

    @Autowired
    IngestSettings ingestSettings;

    @Bean
    public TokenThreadPoolExecutor TokenThreadPoolExecutor() {
        return new TokenThreadPoolExecutor(4, 6, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(10));
    }


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
            b.setTotalFiles(5);
            bagRepository.save(b);
            bagList.add(b);

            System.out.printf("Creating tokens for bag %d...\n", i);
            for (int j = 0; j < 5; j++) {
                AceToken token = new AceToken(b, new Date(), "file-"+j,
                        "proof-"+j, "ims-service", "SHA-256", new Long(j));
                tokenRepository.save(token);
            }
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
