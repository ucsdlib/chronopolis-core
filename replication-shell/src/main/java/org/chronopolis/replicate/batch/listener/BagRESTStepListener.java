package org.chronopolis.replicate.batch.listener;

import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Created by shake on 12/4/14.
 */
public class BagRESTStepListener implements StepExecutionListener {

    private IngestAPI ingestAPI;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    public BagRESTStepListener(IngestAPI ingestAPI,
                               Replication replication,
                               ReplicationSettings settings,
                               ReplicationNotifier notifier) {
        this.ingestAPI = ingestAPI;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
    }

    @Override
    public void beforeStep(final StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        String digest = notifier.getCalculatedTagDigest();

        replication.setReceivedTagFixity(digest);
        ingestAPI.updateReplication(replication.getReplicationID(), replication);

        return ExitStatus.COMPLETED;
    }
}
