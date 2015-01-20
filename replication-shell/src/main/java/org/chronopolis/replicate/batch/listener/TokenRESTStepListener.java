package org.chronopolis.replicate.batch.listener;

import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Created by shake on 12/4/14.
 */
public class TokenRESTStepListener implements StepExecutionListener {

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
        String digest = notifier.getCalculatedTokenDigest();

        replication.setReceivedTokenFixity(digest);
        ingestAPI.updateReplication(replication.getReplicationID(), replication);

        return ExitStatus.COMPLETED;
    }
}
