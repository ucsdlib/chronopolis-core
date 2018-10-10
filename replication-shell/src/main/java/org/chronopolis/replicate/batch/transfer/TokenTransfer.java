package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.callback.UpdateCallback;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Callback;

import java.util.Optional;

/**
 * Class to transfer token stores to a given bucket
 * <p>
 * Created by shake on 10/11/16.
 */
public class TokenTransfer implements Transfer, Runnable {

    // todo: something other than rsync-log
    private final Logger log = LoggerFactory.getLogger("rsync-log");

    private final Long replicationId;
    private final Bucket bucket;
    private final SingleFileOperation operation;
    private final ReplicationService replications;
    private final ReplicationProperties properties;

    public TokenTransfer(Bucket bucket,
                         SingleFileOperation operation,
                         Replication replication,
                         ReplicationService replications,
                         ReplicationProperties properties) {
        this.bucket = bucket;
        this.operation = operation;
        this.replications = replications;
        this.replicationId = replication.getId();
        this.properties = properties;
    }

    @Override
    public void run() {
        String link = operation.getLink();
        String identifier = operation.getIdentifier();
        log.info("{} Downloading Token Store from {}", identifier, link);

        Optional<FileTransfer> transfer;
        switch (operation.getType()) {
            case RSYNC:
                transfer = bucket.transfer(operation, properties.getRsync().getArguments());
                break;
            default:
                transfer = Optional.empty();
        }

        transfer.flatMap(xfer -> transfer(xfer, identifier))
                // For a Token Operation, the operation run is a SingleFileOperation, so we
                // can  use the path given from the operation to get the hash of the token store
                .flatMap(ignored -> bucket.hash(operation, operation.getFile()))
                .map(this::update)
                .orElseThrow(() -> new RuntimeException("Unable to update token store fixity " +
                        "value. Check that the file exists or that the Ingest API is available."));
    }

    @Override
    public Callback<Replication> update(HashCode hash) {
        UpdateCallback cb = new UpdateCallback();
        String calculatedDigest = hash.toString();
        String identifier = operation.getIdentifier();
        log.info("{} Calculated digest {} for token store", identifier, calculatedDigest);

        // could probably extend call and do our own enqueue which returns a callback
        Call<Replication> call = replications.updateTokenStoreFixity(replicationId,
                new FixityUpdate(calculatedDigest));
        call.enqueue(cb);
        return cb;
    }

}
