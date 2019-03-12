package org.chronopolis.replicate.batch;

import com.google.common.annotations.VisibleForTesting;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.ace.AceFactory;
import org.chronopolis.replicate.support.Reporter;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.BiConsumer;

/**
 * Submit a replication for processing
 * <p>
 * TODO: Figure out if we want to eagerly reject replications (i.e., keep our queues bounded)
 * This way we can simulate back pressure, and not worry about running our of memory if
 * we receive too many replications. It just has other implications surrounding how we
 * track which replications are ongoing, and how to handle them. In addition, we'll
 * want to find a way to resubmit operations to our io pool, possibly through a different
 * ExecutorService, but testing will need to be done to see how that plays with the
 * CompletableFuture interface.
 * <p>
 * Also we'll want to reevaluate if we want to do partial flows (replicate; ace register; ace load;
 * ace audit) or if we should keep it how it is now. Either way there will be some updates to make a
 * distinction of when to audit ace data as currently we trigger the audit eagerly, which could be
 * bad if tokens fail to upload.
 * <p>
 * todo: +ingestAPIProperties
 * <p>
 * Created by shake on 10/12/16.
 */
public class Submitter {
    private final Logger log = LoggerFactory.getLogger(Submitter.class);

    private final AceService ace;
    private final Reporter<SimpleMailMessage> reporter;

    private final BucketBroker broker;
    private final ServiceGenerator generator;
    private final AceFactory aceFactory;
    private final TransferFactory transferFactory;

    private final ThreadPoolExecutor http;
    private final ReplicationProperties properties;

    private final Set<String> replicating;

    public Submitter(AceService ace,
                     Reporter<SimpleMailMessage> reporter,
                     BucketBroker broker,
                     ServiceGenerator generator,
                     AceFactory aceFactory,
                     TransferFactory transferFactory,
                     ReplicationProperties properties,
                     ThreadPoolExecutor http) {
        this.reporter = reporter;
        this.ace = ace;
        this.broker = broker;
        this.generator = generator;
        this.aceFactory = aceFactory;
        this.transferFactory = transferFactory;
        this.properties = properties;
        this.http = http;

        this.replicating = new ConcurrentSkipListSet<>();
    }

    @VisibleForTesting
    public boolean isRunning(Replication replication) {
        return replicating.contains(replicationIdentifier(replication));
    }

    /**
     * Basic configuration for a {@link StorageOperation}. Depending on the protocol of the
     * replication allow for different configuration.
     *
     * @param operation   the operation to configure
     * @param replication the replication being processed
     * @param storage     the StagingStorage of either the Bag or TokenStore
     * @param link        the link of either the Bag or TokenStore
     */
    private void configureOperation(StorageOperation operation,
                                    Replication replication,
                                    StagingStorage storage,
                                    String link) {
        operation.setLink(link);
        operation.setSize(storage.getSize());
        operation.setIdentifier(replicationIdentifier(replication));
        operation.setType(OperationType.from(replication.getProtocol()));
    }

    /**
     * Submit a replication for processing, returning the future which it is bound by
     *
     * @param replication the replication to work on
     * @return a {@link CompletableFuture} of the replication flow
     */
    public CompletableFuture<ReplicationStatus> submit(Replication replication) {
        String identifier = replicationIdentifier(replication);

        CompletableFuture<ReplicationStatus> future;
        if (replicating.add(identifier)) {
            log.info("Submitting replication {}", identifier);

            StagingStorage bagStorage = replication.getBag().getBagStorage();
            StagingStorage tokenStorage = replication.getBag().getTokenStorage();
            if (bagStorage != null && tokenStorage != null) {
                // create storage operations
                DirectoryStorageOperation bagOp =
                        new DirectoryStorageOperation(Paths.get(bagStorage.getPath()));
                configureOperation(bagOp, replication, bagStorage, replication.getBagLink());

                SingleFileOperation tokenOp =
                        new SingleFileOperation(Paths.get(tokenStorage.getPath()));
                configureOperation(tokenOp, replication, tokenStorage, replication.getTokenLink());

                // create work based on replication status
                // do we want to run through the entire flow?
                switch (replication.getStatus()) {
                    // Allocate
                    case PENDING:
                        future = allocate(replication, bagOp, tokenOp);
                        break;
                    // Replicate
                    case STARTED:
                        future = fromStarted(replication, bagOp, tokenOp);
                        break;
                    // Do all ACE work
                    case TRANSFERRED:
                    case ACE_REGISTERED:
                    case ACE_TOKEN_LOADED:
                        future = fromTransferred(replication, bagOp, tokenOp);
                        break;
                    case ACE_AUDITING:
                        future = fromAceAuditing(replication);
                        break;
                    default:
                        return null;
                }
            } else {
                // we could throw an exception but that seems kind of unnecessary
                future = CompletableFuture.supplyAsync(() -> {
                    log.error("[{}] Unable to work on replication", identifier);
                    return ReplicationStatus.FAILURE;
                });
            }

            // handle instead of whenComplete?
            return future.whenComplete(new Completer(replication));
        } else {
            log.debug("Replication {} is already running", identifier);
        }

        // just so we don't return null
        return CompletableFuture.supplyAsync(replication::getStatus);
    }

    /**
     * Allocate a bucket for both the BagOperation and TokenOperation (Directory and SingleFile
     * Operations, respectively). If not able to complete, then fail.
     *
     * @param replication the replication being processed
     * @param bagOp       the StorageOperation for replicating a Bag
     * @param tokenOp     the StorageOperation for replicating a TokenStore
     * @return a {@link CompletableFuture} with the updated status of the Replication
     */
    private CompletableFuture<ReplicationStatus> allocate(Replication replication,
                                                          DirectoryStorageOperation bagOp,
                                                          SingleFileOperation tokenOp) {
        Allocator allocator = new Allocator(broker, bagOp, tokenOp, replication, generator);
        return CompletableFuture.supplyAsync(allocator);
    }

    /**
     * Create a CompletableFuture with all steps needed from the PENDING state
     *
     * @param replication the replication to work on
     * @param bagOp       the StorageOperation for replicating a Bag
     * @param tokenOp     the StorageOperation for replicating a TokenStore
     * @return a {@link CompletableFuture} which runs both the token and bag transfers tasks
     */
    private CompletableFuture<ReplicationStatus> fromStarted(Replication replication,
                                                             DirectoryStorageOperation bagOp,
                                                             SingleFileOperation tokenOp) {
        Optional<Bucket> bBucket = broker.findBucketForOperation(bagOp);
        Optional<Bucket> tBucket = broker.findBucketForOperation(tokenOp);
        Optional<MultiBucket> buckets = bBucket.flatMap(bagBucket ->
                tBucket.map(tokenBucket -> new MultiBucket(bagBucket, tokenBucket)));

        Optional<CompletableFuture<Void>> optFuture = buckets.map(multi ->
                CompletableFuture.allOf(
                        transferFactory.tokenTransfer(multi.token, replication, tokenOp),
                        transferFactory.bagTransfer(multi.bag, replication, bagOp)
                )
        );

        return optFuture.map(future ->
                future.thenApplyAsync((Void param) -> replication.getStatus())
        ).orElse(failNotAllocated(replication));
    }

    /**
     * Create a CompletableFuture with all steps needed after a transfer has completed
     *
     * @param replication the replication to work on
     * @return a completable future which runs ace registration tasks
     */
    private CompletableFuture<ReplicationStatus> fromTransferred(
            Replication replication,
            DirectoryStorageOperation bagOp,
            SingleFileOperation tokenOp) {
        Optional<Bucket> bagOpt = broker.findBucketForOperation(bagOp);
        Optional<Bucket> tokenOpt = broker.findBucketForOperation(tokenOp);

        Optional<MultiBucket> multiOpt = bagOpt.flatMap(bagBucket ->
                tokenOpt.map(tokenBucket -> new MultiBucket(bagBucket, tokenBucket)));

        return multiOpt.map(multi ->
                aceFactory.register(replication, multi.bag, bagOp, multi.token, tokenOp)
        ).orElse(failNotAllocated(replication));
    }

    /**
     * Create a CompletableFuture for checking the status of ACE audits
     *
     * @return a {@link CompletableFuture} which runs the AceCheck runnable
     */
    private CompletableFuture<ReplicationStatus> fromAceAuditing(Replication replication) {
        AceCheck check = new AceCheck(ace, generator, replication);
        return CompletableFuture.supplyAsync(check, http);
    }

    private CompletableFuture<ReplicationStatus> failNotAllocated(Replication replication) {
        return CompletableFuture.supplyAsync(() -> {
            log.error("[{}] No bucket allocated for bag!", replication.getBag().getName());
            return ReplicationStatus.FAILURE;
        });
    }

    /**
     * Encapsulate two known buckets
     */
    private class MultiBucket {
        final Bucket bag;
        final Bucket token;

        MultiBucket(Bucket bagBucket, Bucket tokenBucket) {
            this.bag = bagBucket;
            this.token = tokenBucket;
        }
    }

    /**
     * Consumer which runs at the end of a replication, ensures removal from the
     * replicating set
     * <p>
     * eventually switch to a better interface for notifications
     * so we can have multiple outputs. i.e. email, slack, db, etc
     */
    private class Completer implements BiConsumer<ReplicationStatus, Throwable> {
        private final Logger log = LoggerFactory.getLogger(Completer.class);

        final Replication replication;

        Completer(Replication replication) {
            this.replication = replication;
        }

        @Override
        public void accept(ReplicationStatus status, Throwable throwable) {
            String body;
            String subject;
            String replicationId = replicationIdentifier(replication);

            try {
                // Send mail if there is an exception
                if (throwable != null) {
                    log.warn("Replication did not complete successfully, returned throwable is",
                            throwable);
                    subject = "Failed to replicate " + replicationId;
                    body = throwable.getMessage()
                            + "\n"
                            + Arrays.toString(throwable.getStackTrace());
                    send(subject, body);

                    // Send mail if we are set to and the replication is complete
                } else if (properties.getSmtp().getSendOnSuccess() &&
                        status == ReplicationStatus.SUCCESS) {
                    subject = "Successful replication of " + replicationId;
                    body = "";
                    send(subject, body);
                }
            } catch (Exception e) {
                log.error("Exception caught sending mail", e);
            } finally {
                log.debug("{} removing from thread pool", replicationId);
                replicating.remove(replicationId);
            }
        }

        private void send(String subject, String body) {
            SimpleMailMessage message = reporter.createMessage(properties.getNode(), subject, body);
            reporter.send(message);
        }
    }

    private String replicationIdentifier(Replication replication) {
        return replication.getBag().getDepositor() + "/" + replication.getBag().getName();
    }

}
