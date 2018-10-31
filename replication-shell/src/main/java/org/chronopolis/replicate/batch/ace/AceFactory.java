package org.chronopolis.replicate.batch.ace;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Ace to the f u t u r e
 * <p>
 * todo: we'll want to improve how/when we execute tasks, similar to the get/register split
 * <p>
 * specifically:
 * - wait for token registration to complete before issuing an audit
 * - retrying audits
 * - possibly retrieving recent events
 * <p>
 * most of these needs updates to ACE first though so no work can be done yet
 *
 * @author shake
 */
public class AceFactory {

    private final Logger log = LoggerFactory.getLogger(AceFactory.class);

    private final AceService ace;
    private final ServiceGenerator generator;
    private final AceConfiguration aceConfiguration;
    private final ThreadPoolExecutor httpExecutor;

    public AceFactory(AceService ace,
                      ServiceGenerator generator,
                      AceConfiguration aceConfiguration,
                      ThreadPoolExecutor httpExecutor) {
        this.ace = ace;
        this.generator = generator;
        this.aceConfiguration = aceConfiguration;
        this.httpExecutor = httpExecutor;
    }

    public CompletableFuture<ReplicationStatus> register(Replication replication,
                                                         Bucket bagBucket,
                                                         DirectoryStorageOperation bagOperation,
                                                         Bucket tokenBucket,
                                                         SingleFileOperation tokenOperation) {
        if (replication.getStatus().isFailure() ||
                replication.getStatus().ordinal() < ReplicationStatus.TRANSFERRED.ordinal()) {
            return CompletableFuture.supplyAsync(replication::getStatus);
        }

        log.debug("Running Ace tasks");
        final ReplicationNotifier notifier = new ReplicationNotifier(replication);
        final AceRegisterTasklet register = new AceRegisterTasklet(ace,
                replication,
                generator,
                aceConfiguration,
                notifier,
                bagBucket,
                bagOperation);

        return CompletableFuture
                .supplyAsync(register::call, httpExecutor)
                .thenApplyAsync(id -> {
                    if (notifier.isSuccess()) {
                        AceTokenTasklet token = new AceTokenTasklet(tokenBucket,
                                tokenOperation,
                                generator,
                                ace,
                                replication,
                                notifier,
                                id);
                        token.run();
                    }

                    return id;
                }, httpExecutor)
                .thenApplyAsync(id -> {
                    if (notifier.isSuccess()) {
                        AceAuditTasklet audit = new AceAuditTasklet(generator,
                                ace,
                                replication,
                                notifier,
                                id);
                        audit.run();
                    }

                    return replication.getStatus();
                }, httpExecutor);
    }
}
