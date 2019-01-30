package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.BucketBroker;
import org.chronopolis.common.storage.Posix;
import org.chronopolis.common.storage.PosixBucket;
import org.chronopolis.common.storage.PreservationProperties;
import org.chronopolis.replicate.ReplicationProperties;
import org.chronopolis.replicate.batch.ace.AceFactory;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.chronopolis.test.support.CallWrapper;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.chronopolis.rest.models.enums.ReplicationStatus.ACE_AUDITING;
import static org.chronopolis.rest.models.enums.ReplicationStatus.STARTED;
import static org.chronopolis.rest.models.enums.ReplicationStatus.SUCCESS;
import static org.chronopolis.rest.models.enums.ReplicationStatus.TRANSFERRED;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * tests for our submitter for various flows and their outcomes
 * should these be split into multiple classes? (for ease of readability)
 * also we can now test the result of our replication... which could be nice
 * <p>
 * I _really_ don't like the complexity in these tests, and think maybe we should look for a way
 * to scope them a bit better. At the moment we need to set up all sorts of mocking which makes
 * them not only hard to follow, but hard to update, especially as we make changes to the classes
 * which run the steps involved in replicating.
 * <p>
 * Starting with the move to 3.0, I'd like to update these tests to only test the flow and creation
 * of Futures and how failure is handled (catching RuntimeExceptions from the running futures). This
 * will take some time as most of the CompletableFutures are created by the Submitter, but this is
 * starting to be changed... slowly.
 * <p>
 * Created by shake on 10/18/16.
 */
public class SubmitterTest {

    private Submitter submitter;

    private final MailUtil mail = mock(MailUtil.class);
    private final AceService ace = mock(AceService.class);
    private final AceFactory aceFactory = mock(AceFactory.class);
    private final TransferFactory factory = mock(TransferFactory.class);
    private final ReplicationService replications = mock(ReplicationService.class);

    private String testBag;
    private String testToken;

    @Before
    public void setup() throws URISyntaxException, IOException {
        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        Path bags = Paths.get(resources.toURI()).resolve("bags");
        Path tokens = Paths.get(resources.toURI()).resolve("tokens");

        ReplGenerator generator = new ReplGenerator(replications);

        Posix posix = new Posix()
                .setId(1L)
                .setPath(bags.toString());
        Bucket daBucket = new PosixBucket(posix);
        BucketBroker broker = BucketBroker.forBucket(daBucket);

        ReplicationProperties properties = new ReplicationProperties();
        properties.setSmtp(new ReplicationProperties.Smtp().setSendOnSuccess(true));


        testBag = bags.resolve("test-bag").toString() + "/";
        testToken = tokens.resolve("test-token-store").toString();

        PreservationProperties preservation = new PreservationProperties();
        preservation.getPosix().add(new Posix().setPath(bags.toString()));

        // Mock these? No... that wouldn't be good...
        TrackingThreadPoolExecutor<Replication> http
                = new TrackingThreadPoolExecutor<>(1, 1, 1, SECONDS, new LinkedBlockingDeque<>());
        submitter = new Submitter(
                mail, ace, broker, generator, aceFactory, factory, properties, http);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void fromStartedNotAllocated() {
        Bag bag = createBag(testBag.replace("test-bag", "unallocated-bag"), testToken);
        Replication started = createReplication(STARTED, bag);

        CompletableFuture<ReplicationStatus> submit = submitter.submit(started);
        ReplicationStatus status = submit.join();

        Assert.assertFalse(submitter.isRunning(started));
        Assert.assertEquals(ReplicationStatus.FAILURE, status);
    }

    @Test
    @SuppressWarnings("Duplicates")
    public void fromTransferredNotAllocated() {
        Bag bag = createBag(testBag.replace("test-bag", "unallocated-bag"), testToken);

        Replication transferred = createReplication(TRANSFERRED, bag);
        CompletableFuture<ReplicationStatus> submit = submitter.submit(transferred);
        ReplicationStatus status = submit.join();

        Assert.assertFalse(submitter.isRunning(transferred));
        Assert.assertEquals(ReplicationStatus.FAILURE, status);
    }

    @Test
    public void fromStartedFailTokenTransfer() throws ExecutionException, InterruptedException {
        Bag bag = createBag(testBag, testToken.replace("test-token-store", "test-tokens-404"));
        Replication replication = createReplication(STARTED, bag);
        CompletableFuture<Void> xferFuture = CompletableFuture.runAsync(() -> {
        });
        CompletableFuture<Void> xferFail =
                CompletableFuture.runAsync(() -> {
                    throw new RuntimeException("not found");
                });

        when(factory.bagTransfer(any(), any(), any())).thenReturn(xferFuture);
        when(factory.tokenTransfer(any(), any(), any())).thenReturn(xferFail);
        Mockito.doThrow(new MailSendException("Unable to send msg")).when(mail)
                .send(any(SimpleMailMessage.class));
        CompletableFuture<ReplicationStatus> submission = submitter.submit(replication);
        // workaround to block on our submission
        CompletableFuture<Boolean> handle = submission.handle((ok, ex) -> true);
        handle.get();

        assertFalse(submitter.isRunning(replication));
        verify(factory, times(1)).bagTransfer(any(), eq(replication), any());
        verify(factory, times(1)).tokenTransfer(any(), eq(replication), any());
        verify(aceFactory, never()).register(any(), any(), any(), any(), any());
    }

    @Test
    public void fromStartedServerStop() throws InterruptedException, ExecutionException {
        CompletableFuture<Void> xferFuture = CompletableFuture.runAsync(() -> {
        });

        // same issue here as in fromPendingSuccess
        Bag bag = createBag(testBag, testToken);
        Replication replication = createReplication(STARTED, bag);

        // Replication Fixity Update Mock
        when(factory.bagTransfer(any(), any(), any())).thenReturn(xferFuture);
        when(factory.tokenTransfer(any(), any(), any())).thenReturn(xferFuture);

        CompletableFuture<ReplicationStatus> submission = submitter.submit(replication);
        submission.get();

        verify(factory, times(1)).bagTransfer(any(), eq(replication), any());
        verify(factory, times(1)).tokenTransfer(any(), eq(replication), any());
        verify(aceFactory, never()).register(any(), any(), any(), any(), any());
    }

    @Test
    public void fromStartedSuccess() throws InterruptedException, ExecutionException {
        // vars which don't change over the lifetime of the test
        CompletableFuture<Void> xferFuture = CompletableFuture.runAsync(() -> {
        });

        // todo: check to see if mockito can capture the old values
        // Because we need to create an updated replication, there's a bit of ugliness we need to
        // deal with until we have a replication model class as well
        Bag bag = createBag(testBag, testToken);
        Replication replication = createReplication(STARTED, bag);

        when(factory.bagTransfer(any(), eq(replication), any())).thenReturn(xferFuture);
        when(factory.tokenTransfer(any(), eq(replication), any())).thenReturn(xferFuture);

        CompletableFuture<ReplicationStatus> submission = submitter.submit(replication);
        submission.get();

        verify(factory, times(1)).bagTransfer(any(), eq(replication), any());
        verify(factory, times(1)).tokenTransfer(any(), eq(replication), any());
        verify(aceFactory, never()).register(any(), any(), any(), any(), any());
        verify(mail, times(0)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void testAceCheck() throws InterruptedException, ExecutionException {
        Bag bag = createBag(testBag, testToken);
        Replication replication = createReplication(ACE_AUDITING, bag);
        GsonCollection collection = new GsonCollection.Builder()
                .name(bag.getName())
                .group(bag.getDepositor())
                .state("A")
                .storage("local")
                .build();

        when(ace.getCollectionByName(bag.getName(), bag.getDepositor()))
                .thenReturn(new CallWrapper<>(collection));
        when(replications.updateStatus(anyLong(), any(ReplicationStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));
        // look in to being stricter with the createMessage things
        when(mail.createMessage(any(), any(), any())).thenReturn(new SimpleMailMessage());
        CompletableFuture<ReplicationStatus> submission = submitter.submit(replication);
        submission.get();

        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(replications, times(1)).updateStatus(1L, new ReplicationStatusUpdate(SUCCESS));
        verify(mail, times(1)).createMessage(any(), any(), any());
        verify(mail, times(1)).send(any(SimpleMailMessage.class));
    }

    private Replication createReplication(ReplicationStatus status, Bag bag) {
        return new Replication(1L, ZonedDateTime.now(), ZonedDateTime.now(), status,
                bag.getBagStorage().getPath(), bag.getTokenStorage().getPath(),
                "rsync", "", "", "node-user", bag);
    }

    private Bag createBag(String location, String tokens) {
        StagingStorage bagStorage =
                new StagingStorage(true, 10L, 1L, 10L, location, new HashSet<>());
        StagingStorage tokenStorage =
                new StagingStorage(true, 1L, 1L, 10L, tokens, new HashSet<>());
        return new Bag(1L, 1L, 1L, bagStorage, tokenStorage,
                ZonedDateTime.now(), ZonedDateTime.now(),
                "test-name", "submitter-test", "test-depostior",
                BagStatus.REPLICATING, new HashSet<>());
    }


    /**
     * Simple copy of a Replication from Kotlin's copy function
     *
     * @param orig   the original Replication to copy
     * @param status the new status to apply
     * @return a new replication
     */
    private Replication copy(Replication orig, ReplicationStatus status) {
        return orig.copy(orig.getId(),
                orig.getCreatedAt(),
                orig.getUpdatedAt(),
                status,
                orig.getBagLink(),
                orig.getTokenLink(),
                orig.getProtocol(),
                orig.getReceivedTagFixity(),
                orig.getReceivedTokenFixity(),
                orig.getNode(),
                orig.getBag());
    }

}