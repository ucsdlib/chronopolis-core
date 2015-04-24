package org.chronopolis.replicate.config;

import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Autowired
    private JobExplorer explorer;

    @Scheduled(cron = "${replication.cron:0 0 * * * *}")
    public void checkForReplications() {
        Set<String> filter = activeReplications();

        log.info("Query for active replications");
        query(ReplicationStatus.STARTED, filter, false);

        log.info("Query for new replications");
        query(ReplicationStatus.PENDING, filter, true);
    }

    private Set<String> activeReplications() {
        Set<String> filter = new HashSet<>();
        Set<JobExecution> executions = explorer.findRunningJobExecutions("collection-replicate");
        for (JobExecution execution : executions) {
            JobParameters params = execution.getJobParameters();
            String depositor = params.getString("depositor");
            String collection = params.getString("collection");

            filter.add(depositor + ":" + collection);
        }

        return filter;
    }

    private void query(ReplicationStatus status, Set<String> filter, boolean update) {
        List<Replication> replications = ingestAPI.getReplications(status);
        log.debug("Found {} replications", replications.size());

        for (Replication replication : replications) {
            Bag bag = replication.getBag();
            String filterString = bag.getDepositor() + ":" + bag.getName();
            if (update) {
                log.info("Updating replication");
                replication.setStatus(ReplicationStatus.STARTED);
                ingestAPI.updateReplication(replication.getID(), replication);
            }

            // Make sure we don't have a replication already in progress
            if (!filter.contains(filterString)) {
                log.info("Starting job for replication id {}", replication.getID());
                jobStarter.addJobFromRestful(replication);

                // Add our current execution to our filter list
                filter.add(filterString);
            }
        }
    }

}
