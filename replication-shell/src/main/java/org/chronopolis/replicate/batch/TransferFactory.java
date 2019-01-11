package org.chronopolis.replicate.batch;

import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.rsync.ApplyRsync;
import org.chronopolis.replicate.batch.rsync.BagHasher;
import org.chronopolis.replicate.batch.rsync.FilesFromValidator;
import org.chronopolis.replicate.batch.rsync.ListingDownloader;
import org.chronopolis.replicate.batch.rsync.Profile;
import org.chronopolis.replicate.batch.transfer.BagTransfer;
import org.chronopolis.replicate.batch.transfer.TokenTransfer;
import org.chronopolis.rest.api.FileService;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Creator of Futures
 *
 * @author shake
 */
public class TransferFactory {

    private final Logger log = LoggerFactory.getLogger(TransferFactory.class);

    private final FileService files;
    private final ReplicationService replications;
    private final ReplicationProperties properties;
    private final ThreadPoolExecutor longIoPool;

    public TransferFactory(ThreadPoolExecutor longIoPool,
                           FileService files, ReplicationService replications,
                           ReplicationProperties properties) {
        this.files = files;
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
        Profile profile = properties.getRsync().getProfile();
        if (profile == Profile.SINGLE) {
            return CompletableFuture.runAsync(
                    new BagTransfer(bucket, operation, replication, replications, properties),
                    longIoPool);
        } else {
            return chunkedRsync(bucket, replication, operation);
        }
    }

    private CompletableFuture<Void> chunkedRsync(Bucket bucket,
                                                 Replication replication,
                                                 DirectoryStorageOperation operation) {
        BagHasher hasher = new BagHasher(bucket, operation, replication, replications);
        ApplyRsync apply = new ApplyRsync(bucket, operation, replications, properties, longIoPool);
        ListingDownloader downloader =
                new ListingDownloader(replication.getBag(), files, properties);
        return CompletableFuture.supplyAsync(downloader, longIoPool)
                .thenApplyAsync(apply)
                .thenApplyAsync(new FilesFromValidator(replication.getBag(), operation, properties))
                .thenAcceptAsync(hasher);
    }
}
