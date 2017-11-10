package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * Class which transfers bags into a given Bucket
 *
 * Created by shake on 10/11/16.
 */
public class BagTransfer implements Transfer, Runnable {
    private final Logger log = LoggerFactory.getLogger("rsync-log");

    // Fields set by constructor
    private final Bucket bucket;
    private final StorageOperation operation;
    private final ReplicationService replications;

    // These could all be local
    private final Long id;

    public BagTransfer(Bucket bucket, StorageOperation operation, Replication replication, ReplicationService replications) {
        this.bucket = bucket;
        this.operation = operation;
        this.replications = replications;

        this.id = replication.getId();
    }

    @Override
    public void run() {
        // Replicate the collection
        log.info("{} Downloading bag from {}", operation.getIdentifier(), operation.getLink());
        Optional<FileTransfer> transfer = bucket.transfer(operation);

        // This might work better as a CompletableFuture... but this works too
        transfer.flatMap(xfer -> transfer(xfer, operation.getIdentifier()))
                .flatMap(ignored -> bucket.hash(operation, Paths.get("tagmanifest-sha256.txt")))
                .ifPresent(this::update);
    }

    @Override
    public void update(HashCode hash) {
        String calculatedDigest = hash.toString();
        log.info("{} Calculated digest {} for tagmanifest", operation.getIdentifier(), calculatedDigest);

        Call<Replication> call = replications.updateTagManifestFixity(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }
}
