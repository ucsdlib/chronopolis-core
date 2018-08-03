package org.chronopolis.replicate.batch;

import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Allocate storage for operations and update a Replication to be 'started'
 *
 * @author shake
 */
public class Allocator implements Supplier<ReplicationStatus> {

    private final Logger log = LoggerFactory.getLogger(Allocator.class);

    private final BucketBroker broker;
    private final StorageOperation bagOp;
    private final StorageOperation tokenOp;

    private final Replication replication;
    private final ReplicationService replications;

    public Allocator(BucketBroker broker,
                     StorageOperation bagOp,
                     StorageOperation tokenOp,
                     Replication replication,
                     ServiceGenerator generator) {
        this.broker = broker;
        this.bagOp = bagOp;
        this.tokenOp = tokenOp;
        this.replication = replication;
        this.replications = generator.replications();
    }

    @Override
    public ReplicationStatus get() {
        ReplicationStatus status = ReplicationStatus.STARTED;

        log.debug("[{}] Allocating buckets for replication", replication.getBag().getName());
        Bucket bagBucket = broker.findBucketForOperation(bagOp)
                .orElseGet(() -> allocate(bagOp));
        Bucket tokenBucket = broker.findBucketForOperation(tokenOp)
                .orElseGet(() -> allocate(tokenOp));

        Optional<Path> bagDir = bagBucket.mkdir(bagOp);
        Optional<Path> tokenDir = tokenBucket.mkdir(tokenOp);

        if (bagDir.isPresent() && tokenDir.isPresent()) {
            Call<Replication> update = replications.updateStatus(replication.getId(),
                    new ReplicationStatusUpdate(status));
            update.enqueue(new UpdateCallback());
        } else {
            // this should be unreachable, but just in case
            log.warn("[{}] Unable to allocate storage for operations!", replication.getBag().getName());
            status = ReplicationStatus.FAILURE;
        }

        return status;
    }

    private Bucket allocate(StorageOperation operation) {
        return broker.allocateSpaceForOperation(operation)
                .orElseThrow(() -> new IllegalArgumentException("Unable to allocate storage for operation!"));
    }
}
