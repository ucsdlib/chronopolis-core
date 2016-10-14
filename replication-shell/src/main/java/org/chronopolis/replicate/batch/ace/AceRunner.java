package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage the 3 ACE steps we do
 * 1 - ACE_REGISTER
 * 2 - ACE_LOAD
 * 3 - ACE_AUDIT
 *
 * TODO: We may want validation between some of these steps so we know they
 *       completed successfully.
 *
 * Created by shake on 10/13/16.
 */
public class AceRunner implements Runnable {
    private final Logger log = LoggerFactory.getLogger(AceRunner.class);

    final AceService ace;
    final IngestAPI ingest;
    final Replication replication;
    final ReplicationSettings settings;

    // TODO: May be able to remove this
    final ReplicationNotifier notifier;

    public AceRunner(AceService ace, IngestAPI ingest, Replication replication, ReplicationSettings settings, ReplicationNotifier notifier) {
        this.ace = ace;
        this.ingest = ingest;
        this.replication = replication;
        this.settings = settings;
        this.notifier = notifier;
    }

    @Override
    public void run() {
        AceRegisterTasklet register = new AceRegisterTasklet(ingest, ace, replication, settings, notifier);
        Long id = null;
        try {
            id = register.call();
            rest(id);
        } catch (Exception e) {
            log.error("Error communicating with ACE", e);
        }
    }

    private void rest(Long id) {
        // TODO: We will probably want to break this up more - and do some validation along the way
        //       - load tokens + validate we have the expected amount (maybe pull info from ingest)
        //       - run audit
        AceTokenTasklet token = new AceTokenTasklet(ingest, ace, replication, settings, notifier, id);
        AceAuditTasklet audit = new AceAuditTasklet(ingest, ace, replication, settings, notifier, id);
        for (Runnable runnable : ImmutableList.of(token, audit)) {
            if (notifier.isSuccess()) {
                runnable.run();
            }
        }
    }
}
