package org.chronopolis.replicate.batch;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.common.mail.MailUtil;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.NotFoundCallWrapper;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.support.ZonedDateTimeDeserializer;
import org.chronopolis.rest.support.ZonedDateTimeSerializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

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
 *
 * Created by shake on 10/18/16.
 */
public class SubmitterTest {
    // private final Logger log = LoggerFactory.getLogger(SubmitterTest.class);

    private static final String TM_DIGEST = "699caf4dc3dd8bd084f18174035a627b71f31cf5d07d5adbd722c45b874e7a78";
    private static final String TOKEN_DIGEST = "d20b847cbe138983b1235efb607ce9d9a0ba7d5d1d2e95767b3393857ea2cb82";

    Submitter submitter;
    ReplicationSettings settings;

    // Mock these? No... that wouldn't be good...
    TrackingThreadPoolExecutor<Replication> io;
    TrackingThreadPoolExecutor<Replication> http;

    @Mock AceService ace;
    @Mock IngestAPI ingest;
    @Mock MailUtil mail;

    Path bags;
    Path tokens;
    String testBag;
    String testToken;

    Node node;

    @Before
    public void setup() throws URISyntaxException {
        ace = mock(AceService.class);
        ingest = mock(IngestAPI.class);
        mail = mock(MailUtil.class);

        URL resources = ClassLoader.getSystemClassLoader().getResource("");

        settings = new ReplicationSettings();
        settings.setSendOnSuccess(false);
        settings.setPreservation(Paths.get(resources.toURI()).resolve("preservation").toString());

        bags = Paths.get(resources.toURI()).resolve("bags");
        tokens = Paths.get(resources.toURI()).resolve("tokens");

        testBag = bags.resolve("test-bag").toString();
        testToken = tokens.resolve("test-token-store").toString();

        io = new TrackingThreadPoolExecutor<>(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        http = new TrackingThreadPoolExecutor<>(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        submitter = new Submitter(mail, ace, ingest, settings, io, http);

        node = new Node("node-user", "not-a-real-field");
    }

    @Test
    public void fromPendingFailToken() throws ExecutionException, InterruptedException {
        Bag bag = createBag(testBag, testToken.replace("test-token-store", "test-tokens-404"));
        Replication r = createReplication(ReplicationStatus.PENDING, bag);
        FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        Mockito.doThrow(new MailSendException("Unable to send msg")).when(mail).send(any(SimpleMailMessage.class));
        CompletableFuture<Void> submission = submitter.submit(r);
        // workaround to block on our submission
        CompletableFuture<Boolean> handle = submission.handle((ok, ex) -> true);
        handle.get();

        assertFalse(submitter.isRunning(r));
        verify(ingest, times(0)).updateTokenStore(r.getId(), tokenUpdate);
        verify(ingest, times(0)).updateTagManifest(r.getId(), tagUpdate);
        verify(ingest, times(0)).getReplication(r.getId());
        verify(ingest, times(0)).updateReplicationStatus(eq(r.getId()), any(RStatusUpdate.class));
        verify(ace, times(0)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(0)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(0)).startAudit(eq(1L), eq(false));
    }

    @Test
    public void fromPendingServerStop() throws InterruptedException, ExecutionException {
        Bag bag = createBag(testBag, testToken);

        // Because we need to create an updated replication, there's a bit of ugliness we need to
        // deal with until we have a replication model class as well
        Replication r = createReplication(ReplicationStatus.PENDING, bag);
        r.setBag(null);
        Replication updated = copy(r, TypeToken.of(Replication.class).getType());
        r.setBag(bag);
        updated.setBag(bag);

        FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        when(ingest.updateTokenStore(r.getId(), tokenUpdate))
                .thenReturn(new CallWrapper<>(r));
        when(ingest.updateTagManifest(r.getId(), tagUpdate))
                .thenReturn(new CallWrapper<>(r));

        updated.setStatus(ReplicationStatus.FAILURE_TAG_MANIFEST);
        when(ingest.getReplication(r.getId())).thenReturn(new CallWrapper<>(updated));

        CompletableFuture<Void> submission = submitter.submit(r);
        submission.get();

        verify(ingest, times(1)).updateTokenStore(r.getId(), tokenUpdate);
        verify(ingest, times(1)).updateTagManifest(r.getId(), tagUpdate);
        verify(ingest, times(1)).getReplication(r.getId());
        verify(ingest, times(0)).updateReplicationStatus(eq(r.getId()), any(RStatusUpdate.class));
        verify(ace, times(0)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(0)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(0)).startAudit(eq(1L), eq(false));
    }

    @Test
    public void fromPendingSuccess() throws InterruptedException, ExecutionException, IOException, ClassNotFoundException {
        Bag bag = createBag(testBag, testToken);

        // Because we need to create an updated replication, there's a bit of ugliness we need to
        // deal with until we have a replication model class as well
        Replication r = createReplication(ReplicationStatus.PENDING, bag);
        r.setBag(null);
        Replication updated = copy(r, TypeToken.of(Replication.class).getType());
        r.setBag(bag);
        updated.setBag(bag);

        FixityUpdate tokenUpdate = new FixityUpdate(TOKEN_DIGEST);
        FixityUpdate tagUpdate = new FixityUpdate(TM_DIGEST);

        when(ingest.updateTokenStore(r.getId(), tokenUpdate))
                .thenReturn(new CallWrapper<>(r));
        when(ingest.updateTagManifest(r.getId(), tagUpdate))
                .thenReturn(new CallWrapper<>(r));

        // todo: make sure this is the updated replication with status > pending/w.e.
        updated.setStatus(ReplicationStatus.TRANSFERRED);
        when(ingest.getReplication(r.getId())).thenReturn(new CallWrapper<>(updated));
        when(ace.getCollectionByName(bag.getName(), bag.getDepositor())).thenReturn(new NotFoundCallWrapper<>());

        // add + update
        when(ace.addCollection(any(GsonCollection.class))).thenReturn(new CallWrapper<>(ImmutableMap.of("id", 1L)));
        when(ingest.updateReplicationStatus(eq(r.getId()), eq(new RStatusUpdate(ReplicationStatus.ACE_REGISTERED))))
                .thenReturn(new CallWrapper<>(updated));

        // token + update
        when(ace.loadTokenStore(eq(1L), any(RequestBody.class))).thenReturn(new CallWrapper<>(null));
        when(ingest.updateReplicationStatus(eq(r.getId()), eq(new RStatusUpdate(ReplicationStatus.ACE_TOKEN_LOADED))))
                .thenReturn(new CallWrapper<>(updated));

        // audit + update
        when(ace.startAudit(eq(1L), eq(false))).thenReturn(new CallWrapper<>(null));
        when(ingest.updateReplicationStatus(eq(r.getId()), eq(new RStatusUpdate(ReplicationStatus.ACE_AUDITING))))
                .thenReturn(new CallWrapper<>(updated));

        CompletableFuture<Void> submission = submitter.submit(r);
        submission.get();

        verify(ingest, times(1)).updateTokenStore(r.getId(), tokenUpdate);
        verify(ingest, times(1)).updateTagManifest(r.getId(), tagUpdate);
        verify(ingest, times(1)).getReplication(r.getId());
        verify(ingest, times(3)).updateReplicationStatus(eq(r.getId()), any(RStatusUpdate.class));
        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(eq(1L), any(RequestBody.class));
        verify(ace, times(1)).startAudit(eq(1L), eq(false));
    }

    // @Test
    public void fromTransferred() {
    }

    @Test
    public void testAceCheck() throws InterruptedException, ExecutionException {
        Bag bag = createBag(testBag, testToken);
        Replication r = createReplication(ReplicationStatus.ACE_AUDITING, bag);
        GsonCollection c = new GsonCollection.Builder()
                .name(bag.getName())
                .group(bag.getDepositor())
                .state("A")
                .storage("local")
                .build();

        when(ace.getCollectionByName(bag.getName(), bag.getDepositor())).thenReturn(new CallWrapper<>(c));
        when(ingest.updateReplicationStatus(anyLong(), any(RStatusUpdate.class))).thenReturn(new CallWrapper<>(r));
        CompletableFuture<Void> submission = submitter.submit(r);
        submission.get();

        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
        verify(ingest, times(1)).updateReplicationStatus(1L, new RStatusUpdate(ReplicationStatus.SUCCESS));
    }

    Replication createReplication(ReplicationStatus status, Bag bag) {
        Replication r = new Replication();
        r.setId(1L);
        r.setBag(bag);
        r.setBagLink(bag.getLocation());
        r.setTokenLink(bag.getTokenLocation());
        r.setCreatedAt(ZonedDateTime.now());
        r.setUpdatedAt(ZonedDateTime.now());
        r.setNode(node.username);
        r.setProtocol("rsync");
        r.setStatus(status);
        return r;
    }

    Bag createBag(String location, String tokens) {
        Bag bag = new Bag()
                .setName("test-bag")
                .setDepositor("test-depositor")
                .setCreator("submitter-test");
        bag.setId(1L);
        bag.setTokenLocation(tokens);
        bag.setLocation(location);
        bag.setCreatedAt(ZonedDateTime.now());
        bag.setUpdatedAt(ZonedDateTime.now());
        return bag;
    }


    /**
     * Returns a copy of the object, or null if the object cannot
     * be serialized.
     *
     * via: http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
     */
    <T> T copy(T orig, Type type) {
        // log.info("{}", type.getTypeName());
        Gson g = new GsonBuilder()
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer())
                .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeDeserializer())
                .create();
        String json = g.toJson(orig);
        return g.fromJson(json, type);
    }


}