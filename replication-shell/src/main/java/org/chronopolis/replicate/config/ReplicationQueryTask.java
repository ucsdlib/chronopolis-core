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
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scheduled task for checking the ingest-server for replication requests
 * <p/>
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

    /**
     * Check the ingest-server for pending and started replications
     */
    @Scheduled(cron = "${replication.cron:0 0 * * * *}")
    public void checkForReplications() {
        Set<String> filter = activeReplications();

        try {
            log.info("Query for active replications");
            query(ReplicationStatus.STARTED, filter, false);

            log.info("Query for new replications");
            query(ReplicationStatus.PENDING, filter, true);
        } catch (IOException e) {
            log.error("Error checking for replications", e);
        }

    }

    /**
     * Create a set of ongoing replications, specified by
     * depositor:collection
     *
     * @return Set of active replications
     */
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

    /**
     * Query the ingest-server and add requests to the {@link ReplicationJobStarter}
     * if they are not already being replicated
     *
     * @param status - the status of the request to get
     * @param filter - the Set of active replications to filter on
     * @param update - whether or not to update the stats (to STARTED)
     */
    private void query(ReplicationStatus status, Set<String> filter, boolean update) throws IOException {
        int page = 0;
        int pageSize = 20;
        boolean hasNext = true;


        Map<String, Object> params = new HashMap<>();
        params.put("status", status);
        params.put("page_size", pageSize);
        params.put("page", page);

        // TODO: As replications get updated, the state can change and alter the
        // amount of pages. We might want to switch this to only work on one page
        // at a time or figure something else out.
        while (hasNext) {
            Call<Page<Replication>> call = ingestAPI.getReplications(params);
            Response<Page<Replication>> response = call.execute();
            Page<Replication> replications = response.body();
            log.debug("[{}] On page {} with {} replications. {} total.", new Object[]{
                    status,
                    replications.getNumber(),
                    replications.getNumberOfElements(),
                    replications.getTotalElements()});

            ++page;
            hasNext = replications.hasNext();
            params.put("page", page);

            startReplications(replications.getContent(), filter, update);
        }
    }

    private void startReplications(List<Replication> replications, Set<String> filter, boolean update) throws IOException {
        for (Replication replication : replications) {
            log.debug("Replication {} has bag-id {}", replication.getId(), replication.getBagId());
            Call<Bag> call = ingestAPI.getBag(replication.getBagId());
            Response<Bag> response = call.execute();
            Bag bag = response.body();

            replication.setBag(bag);
            String filterString = bag.getDepositor() + ":" + bag.getName();
            if (update) {
                log.info("Updating replication");
                replication.setStatus(ReplicationStatus.STARTED);
                ingestAPI.updateReplication(replication.getId(), replication);
            }

            // Make sure we don't have a replication already in progress
            if (!filter.contains(filterString)) {
                log.info("Starting job for replication id {}", replication.getId());
                jobStarter.addJobFromRestful(replication);

                // Add our current execution to our filter list
                filter.add(filterString);
            } else {
                log.info("Skipping replication {}, already in progress", replication.getId());
            }
        }
    }

}
