package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistribution;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.BagStatus;
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
import java.util.Set;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus;

/**
 * Simple task to create replications for bags which have finished tokenizing
 * TODO: Rename ReplicationCreateTask
 *
 *
 * Created by shake on 2/13/15.
 */
@Component
@EnableScheduling
public class ReplicationTask {
    private final Logger log = LoggerFactory.getLogger(ReplicationTask.class);

    private final IngestSettings settings;
    private final BagRepository bagRepository;
    private final ReplicationRepository replicationRepository;

    @Autowired
    public ReplicationTask(IngestSettings settings, BagRepository bagRepository, ReplicationRepository replicationRepository) {
        this.settings = settings;
        this.bagRepository = bagRepository;
        this.replicationRepository = replicationRepository;
    }

    @Scheduled(cron = "${ingest.cron.request:0 */10 * * * *}")
    public void createReplications() {
        String user = settings.getReplicationUser();
        String server = settings.getStorageServer();
        String bagStage = settings.getRsyncBags();
        String tokenStage = settings.getRsyncTokens();

        Collection<Bag> bags = bagRepository.findByStatus(BagStatus.TOKENIZED);

        // todo: use the replicationservice (dao) for this
        for (Bag bag : bags) {
            // Set up the links for nodes to pull from
            Path tokenPath = Paths.get(tokenStage, bag.getTokenLocation());
            String tokenLink = buildLink(user, server, tokenPath);

            Path bagPath = Paths.get(bagStage, bag.getLocation());
            String bagLink = buildLink(user, server, bagPath);

            // And create the transfer requests
            Set<BagDistribution> distributions = bag.getDistributions();
            List<Replication> repls = new ArrayList<>();
            for (BagDistribution dist : distributions) {
                // Ignore replications which already occurred
                if (dist.getStatus() == BagDistributionStatus.REPLICATE) {
                    continue;
                }

                Node node = dist.getNode();
                log.debug("Creating replication object for node {} for bag {}",
                        node.getUsername(), bag.getId());
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

    private String buildLink(String user, String server, Path file) {
        return new StringBuilder(user)
                    .append("@").append(server)
                    .append(":").append(file.toString())
                    .toString();
    }

}
