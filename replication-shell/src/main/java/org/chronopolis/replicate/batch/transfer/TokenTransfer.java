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
 * Class to transfer token stores to a given bucket
 *
 * Created by shake on 10/11/16.
 */
public class TokenTransfer implements Transfer, Runnable {

    // todo: something other than rsync-log
    private final Logger log = LoggerFactory.getLogger("rsync-log");

    private final Long id;
    private final Bucket bucket;
    private final StorageOperation operation;
    private final ReplicationService replications;

    public TokenTransfer(Bucket bucket, StorageOperation operation, Replication replication, ReplicationService replications) {
        this.bucket = bucket;
        this.operation = operation;
        this.replications = replications;
        this.id = replication.getId();
    }

    @Override
    public void run() {
        log.info("{} Downloading Token Store from {}", operation.getIdentifier(), operation.getLink());

        Optional<FileTransfer> transfer = bucket.transfer(operation);

        transfer.flatMap(xfer -> transfer(xfer, operation.getIdentifier()))
                // For a a Token Operation, the operation path contains the full path to the token store
                // so we join it with an empty path
                .flatMap(ignored -> bucket.hash(operation, Paths.get("")))
                .ifPresent(this::update);
    }

    @Override
    public void update(HashCode hash) {
        String calculatedDigest = hash.toString();
        log.info("{} Calculated digest {} for token store", operation.getIdentifier(), calculatedDigest);

        Call<Replication> call = replications.updateTokenStoreFixity(id, new FixityUpdate(calculatedDigest));
        call.enqueue(new UpdateCallback());
    }

}
