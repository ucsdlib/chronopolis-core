package org.chronopolis.replicate.batch.listener;

import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

/**
 * Created by shake on 12/4/14.
 */
public class BagRESTStepListener implements StepExecutionListener {

    private ReplicationSettings settings;
    private IngestAPI ingestAPI;
    private ReplicationNotifier notifier;

    @Override
    public void beforeStep(final StepExecution stepExecution) {
    }

    @Override
    public ExitStatus afterStep(final StepExecution stepExecution) {
        return null;
    }
}
