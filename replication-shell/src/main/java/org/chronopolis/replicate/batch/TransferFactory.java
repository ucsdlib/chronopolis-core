package org.chronopolis.replicate.batch;

import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.transfer.BagTransfer;
import org.chronopolis.replicate.batch.transfer.TokenTransfer;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Replication;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Creator of Futures
 *
 * @author shake
 */
public class TransferFactory {

    private final ThreadPoolExecutor longIoPool;
    private final ReplicationService replications;
    private final ReplicationProperties properties;

    public TransferFactory(ThreadPoolExecutor longIoPool,
                           ReplicationService replications,
                           ReplicationProperties properties) {
        this.replications = replications;
        this.properties = properties;
        this.longIoPool = longIoPool;
    }

    public CompletableFuture<Void> tokenTransfer(Bucket bucket,
                                                 Replication replication,
                                                 SingleFileOperation operation) {
        return CompletableFuture.runAsync(
                new TokenTransfer(bucket, operation, replication, replications, properties),
                longIoPool);
    }

    public CompletableFuture<Void> bagTransfer(Bucket bucket,
                                               Replication replication,
                                               DirectoryStorageOperation operation) {
        return CompletableFuture.runAsync(
                new BagTransfer(bucket, operation, replication, replications, properties),
                longIoPool);
    }
}
