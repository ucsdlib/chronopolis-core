package org.chronopolis.ingest.config;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Simple task to create replications for bags which have finished tokenizing
 *
 *
 * Created by shake on 2/13/15.
 */
@Component
@EnableScheduling
public class ReplicationTask {
    private final Logger log = LoggerFactory.getLogger(ReplicationTask.class);

    @Autowired
    IngestSettings settings;

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    @Autowired
    ReplicationRepository replicationRepository;

    @Scheduled(cron = "0 */10 * * * *")
    public void createReplications() {
        String user = settings.getExternalUser();
        String server = settings.getStorageServer();
        String bagStage = settings.getBagStage();
        String tokenStage = settings.getTokenStage();

        List<Node> nodes = nodeRepository.findAll();
        Collection<Bag> bags = bagRepository.findByStatus(BagStatus.TOKENIZED);

        for (Bag bag : bags) {
            // Set up the links for nodes to pull from
            Path tokenPath = Paths.get(tokenStage, bag.getTokenLocation());
            String tokenLink = new StringBuilder(user)
                    .append("@").append(server)
                    .append(":").append(tokenPath.toString())
                    .toString();

            Path bagPath = Paths.get(bagStage, bag.getLocation());
            String bagLink = new StringBuilder(user)
                    .append("@").append(server)
                    .append(":").append(bagPath.toString())
                    .toString();

            // And create the transfer requests
            List<Replication> repls = new ArrayList<>();
            for (final Node node : nodes) {
                log.debug("Creating replication object for node {} for bag {}",
                        node.getUsername(), bag.getID());
                Replication replication = new Replication(node, bag, bagLink, tokenLink);
                replication.setProtocol("rsync");
                repls.add(replication);
            }
            replicationRepository.save(repls);

            // And update the status of our bag
            bag.setStatus(BagStatus.REPLICATING);
            bagRepository.save(bag);
        }

    }

}
