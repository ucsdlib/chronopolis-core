package org.chronopolis.replicate.scheduled;

import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.support.BagConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scheduled task for checking the ingest-server for replication requests
 * <p/>
 * Created by shake on 12/10/14.
 */
@Component
@EnableScheduling
public class ReplicationQueryTask {
    private final Logger log = LoggerFactory.getLogger(ReplicationQueryTask.class);

    final IngestAPISettings settings;
    final IngestAPI ingestAPI;
    final Submitter submitter;

    @Autowired
    public ReplicationQueryTask(IngestAPISettings settings, IngestAPI ingest, Submitter submitter) {
        this.settings = settings;
        this.ingestAPI = ingest;
        this.submitter = submitter;
    }

    /**
     * Check the ingest-server for pending and started replications
     * <p>
     */
    @Scheduled(cron = "${replication.cron:0 0 * * * *}")
    public void checkForReplications() {
        Arrays.stream(ReplicationStatus.values())
                .filter(ReplicationStatus::isOngoing) // make sure our replication is part of the valid flow
                .map(this::query)                     // run the query method
                .anyMatch(q -> {                      // short circuit in case we have an exception
                    if (!q.success) {
                        log.error("Error checking for replications", q.t);
                        return true;
                    }

                    return false;
                });
    }

    /**
     * Query the ingest-server and add requests to the {@link Submitter}
     * if they are not already being replicated
     *
     * @param status - the status of the request to get
     */
    private Query query(ReplicationStatus status) {
        log.info("Querying for {} replications", status.toString());

        int page = 0;
        int pageSize = 20;
        boolean hasNext = true;

        Query q = new Query(true);
        Map<String, Object> params = new HashMap<>();
        params.put("page", page);
        params.put("status", status);
        params.put("page_size", pageSize);
        params.put("node", settings.getIngestAPIUsername());

        // TODO: As replications get updated, the state can change and alter the
        // amount of pages. We might want to switch this to only work on one page
        // at a time or figure something else out.
        try {
            while (hasNext) {
                Call<PageImpl<Replication>> call = ingestAPI.getReplications(params);
                Response<PageImpl<Replication>> response = call.execute();
                Page<Replication> replications = response.body();
                log.debug("[{}] On page {} with {} replications. {} total.", new Object[]{
                        status,
                        replications.getNumber(),
                        replications.getNumberOfElements(),
                        replications.getTotalElements()});

                ++page;
                hasNext = replications.hasNext();
                params.put("page", page);

                startReplications(replications.getContent());
            }
        } catch (IOException e) {
            q = new Query(false, e);
        }

        return q;
    }

    private void startReplications(List<Replication> replications) throws IOException {
        for (Replication replication : replications) {
            log.debug("Replication {} has bag-id {}", replication.getId(), replication.getBagId());
            // TODO: It would be nice if the bag was part of the replication. Need to update the API.
            Call<Bag> call = ingestAPI.getBag(replication.getBagId());
            Response<Bag> response = call.execute();
            Bag bag = response.body();
            replication.setBag(BagConverter.toBagEntity(bag));

            submitter.submit(replication);
        }
    }

    private class Query {
        boolean success;
        @Nullable
        Throwable t;

        public Query(boolean success) {
            this.success = success;
        }

        public Query(boolean success, Throwable t) {
            this.success = success;
            this.t = t;
        }
    }

}
