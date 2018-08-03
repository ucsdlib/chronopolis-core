package org.chronopolis.replicate.batch;

import com.google.common.collect.ImmutableMap;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceConfiguration;
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
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.NotFoundCallWrapper;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.kot.api.ReplicationService;
import org.chronopolis.rest.kot.models.Bag;
import org.chronopolis.rest.kot.models.Replication;
import org.chronopolis.rest.kot.models.StagingStorage;
import org.chronopolis.rest.kot.models.enums.BagStatus;
import org.chronopolis.rest.kot.models.enums.ReplicationStatus;
import org.chronopolis.rest.kot.models.update.FixityUpdate;
import org.chronopolis.rest.kot.models.update.ReplicationStatusUpdate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.ACE_AUDITING;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.ACE_REGISTERED;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.ACE_TOKEN_LOADED;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.FAILURE_TAG_MANIFEST;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.STARTED;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.SUCCESS;
import static org.chronopolis.rest.kot.models.enums.ReplicationStatus.TRANSFERRED;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
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
 * One possibility would have these only testing the flow of how things are submitted, and how
 * failure is handled.
 * <p>
 * Created by shake on 10/18/16.
 */
public class SubmitterTest {
    private final Logger log = LoggerFactory.getLogger(SubmitterTest.class);

    private static final String TM_DIGEST = "699caf4dc3dd8bd084f18174035a627b71f31cf5d07d5adbd722c45b874e7a78";
    private static final String TOKEN_DIGEST = "d20b847cbe138983b1235efb607ce9d9a0ba7d5d1d2e95767b3393857ea2cb82";

    private Submitter submitter;

    @Mock
    private AceService ace;
    @Mock
    private MailUtil mail;
    @Mock
    private ReplicationService replications;

    private String testBag;
    private String testToken;

    @Before
    public void setup() throws URISyntaxException, IOException {
        ace = mock(AceService.class);
        mail = mock(MailUtil.class);
        replications = mock(ReplicationService.class);


        URL resources = ClassLoader.getSystemClassLoader().getResource("");
        Path bags = Paths.get(resources.toURI()).resolve("bags");
        Path tokens = Paths.get(resources.toURI()).resolve("tokens");

        ReplGenerator generator = new ReplGenerator(replications);

        Posix posix = new Posix()
                .setId(1L)
                .setPath(bags.toString());
        Bucket daBucket = new PosixBucket(posix);
        BucketBroker broker = BucketBroker.forBucket(daBucket);

        AceConfiguration aceConfiguration = new AceConfiguration();
        ReplicationProperties properties = new ReplicationProperties();
        properties.setSmtp(new ReplicationProperties.Smtp().setSendOnSuccess(true));


        testBag = bags.resolve("test-bag").toString() + "/";
        testToken = tokens.resolve("test-token-store").toString();

        PreservationProperties preservation = new PreservationProperties();
        preservation.getPosix().add(new Posix().setPath(bags.toString()));

        // Mock these? No... that wouldn't be good...
        TrackingThreadPoolExecutor<Replication> io
                = new TrackingThreadPoolExecutor<>(1, 1, 1, SECONDS, new LinkedBlockingDeque<>());
        TrackingThreadPoolExecutor<Replication> http
                = new TrackingThreadPoolExecutor<>(1, 1, 1, SECONDS, new LinkedBlockingDeque<>());
        submitter =
                new Submitter(mail, ace, broker, generator, aceConfiguration, properties, io, http);

        // node = new Node("node-user", "not-a-real-field");
    }

    @Test
    public void fromStartedFailToken() throws ExecutionException, InterruptedException {
        Bag bag = createBag(testBag, testToken.replace("test-token-store", "test-tokens-404"));
        Replication r = createReplication(STARTED, bag);
        FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        Mockito.doThrow(new MailSendException("Unable to send msg")).when(mail)
                .send(any(SimpleMailMessage.class));
        CompletableFuture<ReplicationStatus> submission = submitter.submit(r);
        // workaround to block on our submission
        CompletableFuture<Boolean> handle = submission.handle((ok, ex) -> true);
        handle.get();

        assertFalse(submitter.isRunning(r));
        verify(replications, times(0)).updateTokenStoreFixity(r.getId(), tokenUpdate);
        verify(replications, times(0)).updateTagManifestFixity(r.getId(), tagUpdate);
        verify(replications, times(0)).get(r.getId());
        verify(replications, times(0)).updateStatus(
                eq(r.getId()), any(ReplicationStatusUpdate.class));
        verify(ace, times(0)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(0)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(0)).startAudit(eq(1L), eq(false));
    }

    @Test
    public void fromStartedServerStop() throws InterruptedException, ExecutionException {
        final FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        final FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        // same issue here as in fromPendingSuccess
        Bag bag = createBag(testBag, testToken);
        Replication r = createReplication(STARTED, bag);

        // Return a Bad Fixity
        Replication updated = copy(r, FAILURE_TAG_MANIFEST);

        // Replication Fixity Update Mock
        when(replications.updateTokenStoreFixity(r.getId(), tokenUpdate))
                .thenReturn(new CallWrapper<>(r));
        when(replications.updateTagManifestFixity(r.getId(), tagUpdate))
                .thenReturn(new CallWrapper<>(r));

        when(replications.get(r.getId())).thenReturn(new CallWrapper<>(updated));

        CompletableFuture<ReplicationStatus> submission = submitter.submit(r);
        submission.get();

        verify(replications, times(1)).updateTokenStoreFixity(r.getId(), tokenUpdate);
        verify(replications, times(1)).updateTagManifestFixity(r.getId(), tagUpdate);
        verify(replications, times(1)).get(r.getId());
        verify(replications, times(0)).updateStatus(
                eq(r.getId()), any(ReplicationStatusUpdate.class));
        verify(ace, times(0)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(0)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(0)).startAudit(eq(1L), eq(false));
    }

    @Test
    public void fromStartedSuccess() throws InterruptedException, ExecutionException {
        // vars which don't change over the lifetime of the test
        final FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        final FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        // todo: check to see if mockito can capture the old values
        // Because we need to create an updated replication, there's a bit of ugliness we need to
        // deal with until we have a replication model class as well
        Bag bag = createBag(testBag, testToken);
        Replication r = createReplication(STARTED, bag);

        // Successful transfer -> Status will be set to TRANSFERRED
        Replication updated = copy(r, TRANSFERRED);

        // Replication Fixity Updates
        when(replications.updateTokenStoreFixity(r.getId(), tokenUpdate))
                .thenReturn(new CallWrapper<>(r));
        when(replications.updateTagManifestFixity(r.getId(), tagUpdate))
                .thenReturn(new CallWrapper<>(r));

        when(replications.get(r.getId())).thenReturn(new CallWrapper<>(updated));
        when(ace.getCollectionByName(bag.getName(), bag.getDepositor()))
                .thenReturn(new NotFoundCallWrapper<>());

        // add + update
        when(ace.addCollection(any(GsonCollection.class)))
                .thenReturn(new CallWrapper<>(ImmutableMap.of("id", 1L)));
        when(replications.updateStatus(
                eq(r.getId()), eq(new ReplicationStatusUpdate(ACE_REGISTERED))))
                .thenReturn(new CallWrapper<>(updated));

        // token + update
        when(ace.loadTokenStore(eq(1L), any(RequestBody.class)))
                .thenReturn(new CallWrapper<>(null));
        when(replications.updateStatus(
                eq(r.getId()), eq(new ReplicationStatusUpdate(ACE_TOKEN_LOADED))))
                .thenReturn(new CallWrapper<>(updated));

        // audit + update
        when(ace.startAudit(eq(1L), eq(false))).thenReturn(new CallWrapper<>(null));
        when(replications.updateStatus(
                eq(r.getId()), eq(new ReplicationStatusUpdate(ACE_AUDITING))))
                .thenReturn(new CallWrapper<>(updated));

        CompletableFuture<ReplicationStatus> submission = submitter.submit(r);
        submission.get();

        verify(replications, times(1)).updateTokenStoreFixity(r.getId(), tokenUpdate);
        verify(replications, times(1)).updateTagManifestFixity(r.getId(), tagUpdate);
        verify(replications, times(1)).get(r.getId());
        verify(replications, times(3)).updateStatus(
                eq(r.getId()), any(ReplicationStatusUpdate.class));
        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(1)).startAudit(eq(1L), eq(false));
        verify(mail, times(0)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void testAceCheck() throws InterruptedException, ExecutionException {
        Bag bag = createBag(testBag, testToken);
        Replication r = createReplication(ACE_AUDITING, bag);
        GsonCollection c = new GsonCollection.Builder()
                .name(bag.getName())
                .group(bag.getDepositor())
                .state("A")
                .storage("local")
                .build();

        when(ace.getCollectionByName(bag.getName(), bag.getDepositor()))
                .thenReturn(new CallWrapper<>(c));
        when(replications.updateStatus(anyLong(), any(ReplicationStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(r));
        CompletableFuture<ReplicationStatus> submission = submitter.submit(r);
        submission.get();

        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(replications, times(1)).updateStatus(1L, new ReplicationStatusUpdate(SUCCESS));
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