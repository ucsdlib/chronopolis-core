package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.Supplier;

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
public class AceRunner implements Supplier<ReplicationStatus>, Function<Void, ReplicationStatus> {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    final AceService ace;
    final IngestAPI ingest;
    final Long replicationId;
    final ReplicationSettings settings;

    // TODO: May be able to remove this
    final ReplicationNotifier notifier;

    public AceRunner(AceService ace, IngestAPI ingest, Long replicationId, ReplicationSettings settings, ReplicationNotifier notifier) {
        this.ace = ace;
        this.ingest = ingest;
        this.replicationId = replicationId;
        this.settings = settings;
        this.notifier = notifier;
    }

    @Override
    public ReplicationStatus get() {
        Replication replication = getReplication();
        ReplicationStatus status = ReplicationStatus.ACE_AUDITING;

        // todo: find a cleaner way to do this, possibly by not chaining together all the tasks
        // check if our replication is already terminated
        // or if it hasn't reached transferred
        if (replication == null || replication.getStatus().isFailure()
                || replication.getStatus().ordinal() < ReplicationStatus.TRANSFERRED.ordinal()) {
            return ReplicationStatus.FAILURE;
        }

        AceRegisterTasklet register = new AceRegisterTasklet(ingest, ace, replication, settings, notifier);
        Long id = null;
        try {
            id = register.call();
            rest(id, replication);
        } catch (Exception e) {
            log.error("{} Error communicating with ACE", replication.getBag().getName(), e);
            status = replication.getStatus();
        }

        // This doesn't actually matter at the moment, but this should at least be somewhat sane...
        return status;
    }

    private void rest(Long id, Replication replication) {
        // TODO: We will probably want to break this up more - and do some validation along the way
        //       - load tokens + validate we have the expected amount (maybe pull info from ingest)
        //       - run audit
        AceTokenTasklet token = new AceTokenTasklet(ingest, ace, replication, settings, notifier, id);
        AceAuditTasklet audit = new AceAuditTasklet(ingest, ace, replication, notifier, id);
        for (Runnable runnable : ImmutableList.of(token, audit)) {
            if (notifier.isSuccess()) {
                runnable.run();
            }
        }
    }

    /**
     * Get the replication associated with an id
     *
     * @return the associated replication or null
     */
    private Replication getReplication() {
        Call<Replication> replication = ingest.getReplication(replicationId);
        Replication r = null;
        try {
            Response<Replication> execute = replication.execute();
            if (execute.isSuccessful()) {
                r = execute.body();
            }
        } catch (IOException e) {
            log.error("", e);
        }

        return r;
    }

    // ///???!?!??!?!
    @Override
    public ReplicationStatus apply(Void aVoid) {
        return get();
    }
}
