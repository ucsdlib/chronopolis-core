package org.chronopolis.replicate.batch;

import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.replicate.batch.transfer.BagTransfer;
import org.chronopolis.replicate.batch.transfer.TokenTransfer;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Replication;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Submit a replication for processing
 *
 * Created by shake on 10/12/16.
 */
public class Submitter {

    IngestAPI ingest;
    ReplicationSettings settings;
    Set<String> replicating;
    TrackingThreadPoolExecutor<Replication> io;
    TrackingThreadPoolExecutor<Replication> http;

    public void submit(Replication replication) {
        // idk
        boolean add = replicating.add(replication.getBag().getDepositor() + "/" + replication.getBag().getName());

        if (add) {
            // ...
            CompletableFuture<Void> pending = fromPending(replication);

            // Add a completeable future which removes the replication from the set
        }
    }

    /**
     * Create a CompletableFuture with all steps needed from the PENDING state
     *
     * @param replication the replication to work on
     * @return a completable future which runs both the token and bag transfers tasks
     */
    private CompletableFuture<Void> fromPending(Replication replication) {

        BagTransfer bxfer = new BagTransfer(replication, ingest, settings);
        TokenTransfer txfer = new TokenTransfer(replication, ingest, settings);

        return fromTransferred(CompletableFuture.runAsync(txfer, io)
                .thenRunAsync(bxfer, io)
                , replication);

        // hmmm... how to do this properly
        // return fromTransferred(start, replication, notifier);
    }

    /**
     * Create a CompletableFuture with all steps needed after a transfer has completed
     *
     * @param future the future to append to or null
     * @param replication the replication to work on
     * @return a completable future which runs ace registration tasks
     */
    private CompletableFuture<Void> fromTransferred(@Nullable CompletableFuture<Void> future, Replication replication) {
        if (future == null) {
            return CompletableFuture.runAsync(() -> {}, http);
        }

        return future.thenRunAsync(() -> {}, http);
    }

    /**
     * Create a CompletableFuture for checking the status of ACE audits
     *
     * @return a completable future which runs the AceCheck runnable
     */
    private CompletableFuture<Void> fromAceAuditing(Replication replication) {
        return CompletableFuture.runAsync(() -> {}, http);
    }

}
