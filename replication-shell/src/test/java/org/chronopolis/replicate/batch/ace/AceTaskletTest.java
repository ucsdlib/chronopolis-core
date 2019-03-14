package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceConfiguration;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.storage.Bucket;
import org.chronopolis.common.storage.DirectoryStorageOperation;
import org.chronopolis.common.storage.SingleFileOperation;
import org.chronopolis.common.storage.StorageOperation;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.StagingStorage;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.chronopolis.test.support.CallWrapper;
import org.chronopolis.test.support.ErrorCallWrapper;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static java.time.ZonedDateTime.now;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by shake on 3/18/16.
 */
public class AceTaskletTest {

    // we call stub the same mock multiple times in most tests, so it's best to set lenient here
    // and use strict where applicable
    @Rule public MockitoRule rule = MockitoJUnit.rule().strictness(Strictness.LENIENT);

    private final String name = "test-bag";
    private final String group = "test-depositor";

    @Mock private AceService ace;
    @Mock private ReplicationService replications;
    @Mock private Bucket bagBucket;
    @Mock private Bucket tokenBucket;

    private Bag bag;
    private AceFactory factory;
    private Replication replication;

    private Path tokens;

    @Before
    public void setup() throws URISyntaxException {
        StagingStorage tokenStorage =
                new StagingStorage(true, 1L, 1L, 1L, "tokens/test-token-store", new HashSet<>());
        bag = new Bag(1L, 1L, 1L, null, tokenStorage, now(), now(), name, group, group,
                BagStatus.REPLICATING, new HashSet<>());

        URL bags = ClassLoader.getSystemClassLoader().getResource("");
        tokens = Paths.get(bags.toURI()).resolve(bag.getTokenStorage().getPath());
        AceConfiguration aceConfiguration = new AceConfiguration();
        ServiceGenerator generator = new ReplGenerator(replications);
        ThreadPoolExecutor executor =
                new ThreadPoolExecutor(4, 4, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());

        factory = new AceFactory(ace, generator, aceConfiguration, executor);
    }


    @Test
    public void testAllRun() {
        replication = replication(ReplicationStatus.TRANSFERRED);
        DirectoryStorageOperation bagOp = new DirectoryStorageOperation(Paths.get(group, name));
        SingleFileOperation tokenOp = new SingleFileOperation(Paths.get(group, "test-token-store"));

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareACERegister();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_AUDITING);

        CompletableFuture<ReplicationStatus> future =
                factory.register(replication, bagBucket, bagOp, tokenBucket, tokenOp);
        future.join();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(3)).updateStatus(eq(replication.getId()), any(ReplicationStatusUpdate.class));
    }

    @Test
    public void testAllRunWithCollection() {
        replication = replication(ReplicationStatus.TRANSFERRED);
        DirectoryStorageOperation bagOp = new DirectoryStorageOperation(Paths.get(group, name));
        SingleFileOperation tokenOp = new SingleFileOperation(Paths.get(group, "test-token-store"));

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareAceGet();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_AUDITING);

        CompletableFuture<ReplicationStatus> future =
                factory.register(replication, bagBucket, bagOp, tokenBucket, tokenOp);
        future.join();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(2)).updateStatus(anyLong(), any(ReplicationStatusUpdate.class));
    }

    @Test
    public void testFromTokenLoaded() {
        rule.strictness(Strictness.STRICT_STUBS);

        replication = replication(ReplicationStatus.ACE_TOKEN_LOADED);
        DirectoryStorageOperation bagOp = new DirectoryStorageOperation(Paths.get(group, name));
        SingleFileOperation tokenOp = new SingleFileOperation(Paths.get(group, "test-token-store"));

        // setup our mocks for our http requests
        prepareAceGet();
        prepareAceAudit();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_AUDITING);

        CompletableFuture<ReplicationStatus> future =
                factory.register(replication, bagBucket, bagOp, tokenBucket, tokenOp);
        future.join();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(1)).updateStatus(anyLong(), any(ReplicationStatusUpdate.class));
    }

    @Test
    public void testFromRegistered() {
        replication = replication(ReplicationStatus.ACE_REGISTERED);
        DirectoryStorageOperation bagOp = new DirectoryStorageOperation(Paths.get(group, name));
        SingleFileOperation tokenOp = new SingleFileOperation(Paths.get(group, "test-token-store"));

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareAceGet();
        prepareAceTokenLoad();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(replication.getId(), ReplicationStatus.ACE_AUDITING);

        CompletableFuture<ReplicationStatus> future =
                factory.register(replication, bagBucket, bagOp, tokenBucket, tokenOp);
        future.join();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(2)).updateStatus(anyLong(), any(ReplicationStatusUpdate.class));
    }

    // Helper methods for preparing mocks
    private Replication replication(ReplicationStatus status) {
        return new Replication(1L, now(), now(), status,
                "bag-link", "token-link", "test-protocol", "received-fixity", "received-fixity",
                "test-node", bag);
    }

    private void prepareACERegister() {
        when(bagBucket.fillAceStorage(any(StorageOperation.class), any(GsonCollection.Builder.class)))
                .thenAnswer((Answer<GsonCollection.Builder>) invocation ->
                        invocation.getArgument(1));
        when(ace.getCollectionByName(any(String.class), any(String.class)))
                .thenReturn(new ErrorCallWrapper<>(null, 404, "Not Found"));
        when(ace.addCollection(any(GsonCollection.class)))
                .thenReturn(new CallWrapper<>(ImmutableMap.of("id", 1L)));
    }

    private void prepareIngestGet(Long id, Replication r) {
        when(replications.get(id)).thenReturn(new CallWrapper<>(r));
    }

    private void prepareIngestUpdate(Long id, ReplicationStatus status) {
        ReplicationStatusUpdate update = new ReplicationStatusUpdate(status);
        when(replications.updateStatus(eq(id), eq(update)))
                .thenReturn(new CallWrapper<>(replication));
    }

    private void prepareAceTokenLoad() {
        when(tokenBucket.stream(any(StorageOperation.class), any(Path.class)))
                .thenReturn(Optional.of(Files.asByteSource(tokens.toFile())));
        when(ace.loadTokenStore(anyLong(), any(RequestBody.class)))
                .thenReturn(new CallWrapper<>(null));
    }

    private void prepareAceAudit() {
        when(ace.startAudit(anyLong(), eq(false)))
                .thenReturn(new CallWrapper<>(null));
    }

    private void prepareAceGet() {
        GsonCollection collection = new GsonCollection.Builder()
                .name("test-bag")
                .group("test-depositor")
                .state("A")
                .build();
        collection.setId(1L);

        when(ace.getCollectionByName("test-bag", "test-depositor"))
                .thenReturn(new AsyncWrapper<>(collection));
    }

    // Helper class for handling Calls

    /**
     * Class to attempt to replicate a longer response from a server
     * Waits 2 seconds before updating the callback, used to make sure our phaser
     * is correct
     * <p>
     * TODO: We'll want to do this for the NotFoundCW as well
     *
     * @param <E>
     */
    private class AsyncWrapper<E> extends CallWrapper<E> {
        final E e;

        public AsyncWrapper(E e) {
            super(e);
            this.e = e;
        }

        @Override
        public void enqueue(Callback<E> callback) {
            Thread thread = new Thread(() -> {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException ignored) {
                }

                callback.onResponse(new AsyncWrapper<>(e), Response.success(e));
            });

            thread.start();
        }
    }

}