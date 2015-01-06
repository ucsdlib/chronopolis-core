package org.chronopolis.replicate.config;

import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by shake on 12/10/14.
 */
@Component
@EnableScheduling
public class ReplicationQueryTask {
    private final Logger log = LoggerFactory.getLogger(ReplicationQueryTask.class);

    @Autowired
    private IngestAPI ingestAPI;

    @Autowired
    private ReplicationJobStarter jobStarter;

    @Scheduled(cron = "0 0 * * * *")
    public void checkForReplications() {
        log.info("Query for active replications");
        query(ReplicationStatus.STARTED, false);

        log.info("Query for new replications");
        query(ReplicationStatus.PENDING, true);
    }


    private void query(ReplicationStatus status, boolean update) {
        List<Replication> replications = ingestAPI.getReplications(status);
        log.debug("Found {} replications", replications.size());

        for (Replication replication : replications) {
            log.info("Starting job for replication id {}", replication.getReplicationID());
            if (update) {
                log.info("Updating replication");
                replication.setStatus(ReplicationStatus.STARTED);
                ingestAPI.updateReplication(replication.getReplicationID(), replication);
            }
            jobStarter.addJobFromRestful(replication);
        }
    }

}
