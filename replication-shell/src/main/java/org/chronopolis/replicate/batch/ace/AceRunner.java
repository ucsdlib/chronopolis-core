package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableList;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.ReplicationStatus;
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
 * Might want to revisit this and see if we can clean it up
 *
 * TODO: We may want validation between some of these steps so we know they
 *       completed successfully.
 *
 * Created by shake on 10/13/16.
 */
public class AceRunner implements Supplier<ReplicationStatus>, Function<Void, ReplicationStatus> {
    private final Logger log = LoggerFactory.getLogger("ace-log");

    private final AceService ace;
    private final Long replicationId;
    private final AceConfiguration aceConfiguration;

    // these could be put into a single class which we can get them with... idk... not a big deal imo
    private final Bucket bagBucket;
    private final Bucket tokenBucket;
    private final DirectoryStorageOperation bagOp;
    private final SingleFileOperation tokenOp;

    private final ServiceGenerator generator;

    private final ReplicationNotifier notifier;

    public AceRunner(AceService ace,
                     ServiceGenerator generator,
                     Long replicationId,
                     AceConfiguration aceConfiguration,
                     Bucket bagBucket,
                     Bucket tokenBucket,
                     DirectoryStorageOperation bagOp,
                     SingleFileOperation tokenOp,
                     ReplicationNotifier notifier) {
        this.ace = ace;
        this.tokenBucket = tokenBucket;
        this.tokenOp = tokenOp;
        this.bagBucket = bagBucket;
        this.bagOp = bagOp;
        this.notifier = notifier;
        this.generator = generator;
        this.replicationId = replicationId;
        this.aceConfiguration = aceConfiguration;
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

        // any op needed...?
        AceRegisterTasklet register = new AceRegisterTasklet(ace, replication, generator, aceConfiguration, notifier, bagBucket, bagOp);
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

        // tokens -> tokenBucket + tokenOp?
        // audit -> noBucket + noOp (not to be confused with 0x90)
        AceTokenTasklet token = new AceTokenTasklet(tokenBucket, tokenOp, generator, ace, replication, notifier, id);
        AceAuditTasklet audit = new AceAuditTasklet(generator, ace, replication, notifier, id);
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
        ReplicationService replications = generator.replications();
        Call<Replication> replication = replications.get(replicationId);
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
