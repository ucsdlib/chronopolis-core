package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

/**
 * @deprecated will be removed by 1.4.0-RELEASE
 * Fuckin' tasklet to manage the 3 ACE steps we do
 * 1 - ACE_REGISTER
 * 2 - ACE_LOAD
 * 3 - ACE_AUDIT
 *
 * Created by shake on 3/9/16.
 */
@Deprecated
public class AceTasklet implements Tasklet {
    private final Logger log = LoggerFactory.getLogger(AceTasklet.class);

    private IngestAPI ingest;
    private AceService aceService;
    private Replication replication;
    private ReplicationSettings settings;
    private ReplicationNotifier notifier;

    public AceTasklet(IngestAPI ingest, AceService aceService, Replication replication, ReplicationSettings settings, ReplicationNotifier notifier) {
        this.ingest = ingest;
        this.aceService = aceService;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
    }

    @Override
    public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
        AceRegisterTasklet register = new AceRegisterTasklet(ingest, aceService, replication, settings, notifier);
        Long id = register.call();

        // TODO: We will probably want to break this up more - and do some validation along the way
        //       - load tokens + validate we have the expected amount (maybe pull info from ingest)
        //       - run audit
        AceTokenTasklet token = new AceTokenTasklet(ingest, aceService, replication, settings, notifier, id);
        AceAuditTasklet audit = new AceAuditTasklet(ingest, aceService, replication, settings, notifier, id);
        for (Runnable runnable : ImmutableList.of(token, audit)) {
            if (notifier.isSuccess()) {
                runnable.run();
            }
        }

        return RepeatStatus.FINISHED;
    }

}
