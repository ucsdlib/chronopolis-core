package org.chronopolis.replicate.batch.listener;

import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Created by shake on 12/4/14.
 */
public class TokenRESTStepListener implements StepExecutionListener {
    private final Logger log = LoggerFactory.getLogger(TokenRESTStepListener.class);

    private IngestAPI ingestAPI;
    private Replication replication;
    private ReplicationNotifier notifier;

    public TokenRESTStepListener(IngestAPI ingestAPI,
                                 Replication replication,
                                 ReplicationNotifier notifier) {
        this.ingestAPI = ingestAPI;
        this.replication = replication;
        this.notifier = notifier;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {

    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        // Check if we were able to download, if not let the ingest-server know
        if (notifier.isSuccess()) {
            log.trace("successful download");
            String digest = notifier.getCalculatedTokenDigest();

            replication.setReceivedTokenFixity(digest);
            Replication updated = ingestAPI.updateReplication(replication.getID(), replication);
            if (updated.getStatus() == ReplicationStatus.FAILURE_TOKEN_STORE) {
                log.error("Error validating token store");
                // stop the execution
                stepExecution.upgradeStatus(BatchStatus.STOPPED);
                return ExitStatus.FAILED;
            }
        } else {
            log.trace("unsuccessful download");
            // General failure
            replication.setStatus(ReplicationStatus.FAILURE);
            ingestAPI.updateReplication(replication.getID(), replication);
        }

        return ExitStatus.COMPLETED;
    }
}
