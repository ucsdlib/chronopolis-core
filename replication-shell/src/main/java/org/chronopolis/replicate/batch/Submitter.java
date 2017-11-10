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
 * TODO: Figure out if we want to eagerly reject replications (i.e., keep our queues bounded)
 * This way we can simulate back pressure, and not worry about running our of memory if
 * we receive too many replications. It just has other implications surrounding how we
 * track which replications are ongoing, and how to handle them. In addition, we'll
 * want to find a way to resubmit operations to our io pool, possibly through a different
 * ExecutorService, but testing will need to be done to see how that plays with the
 * CompletableFuture interface.
 *
 * todo: +ingestAPIProperties
 *
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

            StorageOperation bagOp = createOperation(replication, replication.getBagLink(), replication.getBag().getBagStorage(), identifier);
            StorageOperation tokenOp = createOperation(replication, replication.getTokenLink(), replication.getBag().getTokenStorage(), identifier);

            // tf will this do on an exception?
            // we could try to differentiate between allocate and find... but then we deal with so many fsking optionals
            // trying to think of the best way to handle all of this
            // could also do it in each runnable but... eh
            Bucket bagBucket = broker.allocateSpaceForOperation(bagOp)
                    .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for bag!"));
            Bucket tokenBucket = broker.allocateSpaceForOperation(tokenOp)
                    .orElseThrow(() -> new IllegalArgumentException("No bucket allocated for token!"));

            CompletableFuture<ReplicationStatus> future;
            switch (replication.getStatus()) {
                // Run through the full flow
                case PENDING:
                case STARTED:
                    future = fromPending(replication, bagOp, bagBucket, tokenOp, tokenBucket);
                    break;
                // Do all ACE work
                case TRANSFERRED:
                case ACE_REGISTERED:
                case ACE_TOKEN_LOADED:
                    future = fromTransferred(null, replication, bagOp, bagBucket, tokenOp, tokenBucket);
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

    private StorageOperation createOperation(Replication replication, String identifier, StagingStorageModel staging, String link) {
        return new StorageOperation()
                .setLink(link)
                .setIdentifier(identifier)
                .setSize(staging.getSize())
                .setPath(Paths.get(replication.getBag().getDepositor()))
                .setType(OperationType.valueOf(replication.getProtocol()));
    }

    /**
     * Create a CompletableFuture with all steps needed from the PENDING state
     *
     * @param replication the replication to work on
     * @param bagOp
     * @param bagBucket
     * @param tokenOp
     * @param tokenBucket
     * @return a {@link CompletableFuture} which runs both the token and bag transfers tasks
     */
    private CompletableFuture<ReplicationStatus> fromPending(Replication replication,
                                                             StorageOperation bagOp,
                                                             Bucket bagBucket,
                                                             StorageOperation tokenOp,
                                                             Bucket tokenBucket) {
        // todo: test with optionals, see if we can do something with composability
        // Optional<Bucket> bOptional = Optional.of(bagBucket);
        // Optional<Bucket> tOptional = Optional.of(tokenBucket);
        // String id = replicationIdentifier(replication);

        ReplicationService replications = generator.replications();
        BagTransfer bxfer = new BagTransfer(bagBucket, bagOp, replication, replications);
        TokenTransfer txfer = new TokenTransfer(tokenBucket, tokenOp, replication, replications);

        // todo: maybe we shouldn't chain together fromTransferred?
        return fromTransferred(
                CompletableFuture
                        .runAsync(txfer, io)
                        .thenRunAsync(bxfer, io),
                replication, bagOp, bagBucket, tokenOp, tokenBucket);
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
                                                                 Bucket bagBucket,
                                                                 StorageOperation tokenOp,
                                                                 Bucket tokenBucket) {
        ReplicationNotifier notifier = new ReplicationNotifier(replication);
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
