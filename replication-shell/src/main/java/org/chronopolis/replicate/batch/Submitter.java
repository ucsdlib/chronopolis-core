package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.OperationType;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.ace.AceRunner;
import org.chronopolis.replicate.batch.transfer.BagTransfer;
import org.chronopolis.replicate.batch.transfer.TokenTransfer;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;

import javax.annotation.Nullable;
import java.nio.file.Path;
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
 * Also we'll want to reevaluate if we want to do partial flows (replicate; ace register; ace load; ace audit)
 * or if we should keep it how it is now. Either way there will be some updates to make a distinction of
 * when to audit ace data as currently we trigger the audit eagerly, which could be bad if tokens fail to
 * upload.
 * <p>
 * <p>
 * todo: +ingestAPIProperties
 * <p>
 * <p>
 * Created by shake on 10/12/16.
 */
public class Submitter {
    private final Logger log = LoggerFactory.getLogger(Submitter.class);

    private final MailUtil mail;
    private final AceService ace;

    private final BucketBroker broker;
    private final ServiceGenerator generator;

    private final ReplicationProperties properties;
    private final ThreadPoolExecutor io;
    private final ThreadPoolExecutor http;

    private final Set<String> replicating;
    private final AceConfiguration aceConfiguration;

    @Autowired
    public Submitter(MailUtil mail,
                     AceService ace,
                     BucketBroker broker,
                     ServiceGenerator generator,
                     AceConfiguration aceConfiguration,
                     ReplicationProperties properties,
                     ThreadPoolExecutor io,
                     ThreadPoolExecutor http) {
        this.mail = mail;
        this.ace = ace;
        this.broker = broker;
        this.generator = generator;
        this.aceConfiguration = aceConfiguration;
        this.properties = properties;
        this.io = io;
        this.http = http;

        this.replicating = new ConcurrentSkipListSet<>();
    }

    public boolean isRunning(Replication replication) {
        return replicating.contains(replicationIdentifier(replication));
    }

    /**
     * submit a replication for processing, returning the future which it is bound by
     * todo: {@link Optional}
     *
     * @param replication the replication to work on
     * @return a {@link CompletableFuture} of the replication flow
     */
    public CompletableFuture<ReplicationStatus> submit(Replication replication) {
        String identifier = replicationIdentifier(replication);

        // todo: do we want to run through the entire flow or have it staggered like before?
        if (replicating.add(identifier)) {
            log.info("Submitting replication {}", identifier);
            StorageOperation bagOp = createOperation(replication,
                    identifier,
                    replication.getBag().getBagStorage(),
                    replication.getBagLink());
            StorageOperation tokenOp = createOperation(replication,
                    identifier,
                    replication.getBag().getTokenStorage(),
                    replication.getTokenLink());

            CompletableFuture<ReplicationStatus> future;
            switch (replication.getStatus()) {
                // Run through the full flow
                case PENDING:
                case STARTED:
                    future = fromPending(replication, bagOp, tokenOp);
                    break;
                // Do all ACE work
                case TRANSFERRED:
                case ACE_REGISTERED:
                case ACE_TOKEN_LOADED:
                    future = fromTransferred(null, replication, bagOp, tokenOp);
                    break;
                case ACE_AUDITING:
                    future = fromAceAuditing(replication);
                    break;
                default:
                    return null;
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
     * Create a StorageOperation for a given Replication, identifier, StagingStorage, and link
     * <p>
     * Because we have two different transfers which need to be made, we pass in extra information in order to
     * update some of the StorageOperation fields. This way we can choose between the TokenStorage and BagStorage,
     * and get separate links/sizes for both.
     * <p>
     * In addition, when setting the Path for the Operation, we want to make sure it allows for the hashing/stream/ace
     * methods to be used by the Bucket. Basically, for a Bag Operation we want to point to the root of the bag:
     * "depositor/bag_name", and for a Token Operation we simply point at the TokenStore: "depositor/token_store_name".
     * For both of these we can extract the bag_name or token_store_name from the final child of the StagingStorageModel path.
     *
     * @param replication the replication to create an operation for
     * @param identifier  an identifier for the operation
     * @param staging     the StagingStorageModel to determine information about the size and path of the operation
     * @param link        the link for the StorageOperation (will likely be replaced post-2.1.0)
     * @return the StorageOperation
     */
    private StorageOperation createOperation(Replication replication, String identifier, StagingStorageModel staging, String link) {
        Path relative = Paths.get(staging.getPath()).getFileName();
        return new StorageOperation()
                .setLink(link)
                .setIdentifier(identifier)
                .setSize(staging.getSize())
                .setPath(Paths.get(replication.getBag().getDepositor()).resolve(relative))
                .setType(OperationType.from(replication.getProtocol()));
    }

    /**
     * Create a CompletableFuture with all steps needed from the PENDING state
     *
     * @param replication the replication to work on
     * @param bagOp       the StorageOperation for replicating a Bag
     * @param tokenOp     the StorageOperation for replicating a TokenStore
     * @return a {@link CompletableFuture} which runs both the token and bag transfers tasks
     */
    private CompletableFuture<ReplicationStatus> fromPending(Replication replication, StorageOperation bagOp, StorageOperation tokenOp) {
        // tf will this do on an exception?
        // not sure if this is the best way but... hey
        Bucket bagBucket = broker.allocateSpaceForOperation(bagOp)
                .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for bag!"));
        Bucket tokenBucket = broker.allocateSpaceForOperation(tokenOp)
                .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for token!"));


        ReplicationService replications = generator.replications();
        BagTransfer bxfer = new BagTransfer(bagBucket, bagOp, replication, replications);
        TokenTransfer txfer = new TokenTransfer(tokenBucket, tokenOp, replication, replications);

        // todo: maybe we shouldn't chain together fromTransferred?
        return fromTransferred(
                CompletableFuture
                        .runAsync(txfer, io)
                        .thenRunAsync(bxfer, io),
                replication, bagOp, tokenOp);
    }

    /**
     * Create a CompletableFuture with all steps needed after a transfer has completed
     *
     * @param future      the future to append to or null
     * @param replication the replication to work on
     * @return a completable future which runs ace registration tasks
     */
    private CompletableFuture<ReplicationStatus> fromTransferred(@Nullable CompletableFuture<Void> future,
                                                                 Replication replication,
                                                                 StorageOperation bagOp,
                                                                 StorageOperation tokenOp) {
        ReplicationNotifier notifier = new ReplicationNotifier(replication);

        // tf will this do on an exception?
        // could also do it in each runnable but... eh
        Bucket bagBucket = broker.findBucketForOperation(bagOp)
                .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for bag!"));
        Bucket tokenBucket = broker.findBucketForOperation(tokenOp)
                .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for token!"));

        AceRunner runner = new AceRunner(ace, generator, replication.getId(), aceConfiguration, bagBucket, tokenBucket, bagOp, tokenOp, notifier);
        if (future == null) {
            return CompletableFuture.supplyAsync(runner, http);
        }

        return future.thenApplyAsync(runner, http);
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

        public Completer(Replication replication) {
            this.replication = replication;
        }

        @Override
        public void accept(ReplicationStatus status, Throwable throwable) {
            String s = replicationIdentifier(replication);
            String body;
            String subject;

            try {
                // Send mail if there's an exception
                if (throwable != null) {
                    log.warn("Replication did not complete successfully, returned throwable is", throwable);
                    subject = "Failed to replicate " + s;
                    body = throwable.getMessage()
                            + "\n"
                            + Arrays.toString(throwable.getStackTrace());
                    send(subject, body);

                    // Send mail if we are set to and the replication is complete
                } else if (properties.getSmtp().getSendOnSuccess() && status == ReplicationStatus.SUCCESS) {
                    subject = "Successful replication of " + s;
                    body = "";
                    send(subject, body);
                }
            } catch (Exception e) {
                log.error("Exception caught sending mail", e);
            } finally {
                log.debug("{} removing from threadpool", s);
                replicating.remove(s);
            }
        }

        private void send(String subject, String body) {
            SimpleMailMessage message = mail.createMessage(properties.getNode(), subject, body);
            mail.send(message);
        }
    }

    private String replicationIdentifier(Replication replication) {
        return replication.getBag().getDepositor() + "/" + replication.getBag().getName();
    }

}
