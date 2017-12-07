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
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.NotFoundCallWrapper;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.models.storage.StagingStorageModel;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 3/18/16.
 */
public class AceTaskletTest {

    private final String name = "test-bag";
    private final String group = "test-depositor";

    @Mock private AceService ace;
    @Mock private ReplicationService replications;
    @Mock private Bucket bagBucket;
    @Mock private Bucket tokenBucket;

    private Bag b;
    private Node n;
    private Replication replication;
    private ReplicationNotifier notifier;
    private AceConfiguration aceConfiguration;
    private DirectoryStorageOperation bagOp;
    private SingleFileOperation tokenOp;
    private ServiceGenerator generator;

    private Path tokens;

    @Before
    public void setup() throws NoSuchFieldException, URISyntaxException {
        MockitoAnnotations.initMocks(this);

        b = new Bag().setName(name).setDepositor(group);
        b.setTokenStorage(new StagingStorageModel().setPath("tokens/test-token-store"));
        n = new Node("test-node", "test-node-pass");

        URL bags = ClassLoader.getSystemClassLoader().getResource("");
        tokens = Paths.get(bags.toURI()).resolve(b.getTokenStorage().getPath());
        aceConfiguration = new AceConfiguration();
        bagOp = new DirectoryStorageOperation(Paths.get(group, name));
        tokenOp = new SingleFileOperation(Paths.get(group, "test-token-store"));
        generator = new ReplGenerator(replications);
    }


    @Test
    public void testAllRun() {
        replication = new Replication();
        replication.setBag(b);
        replication.setId(1L);
        replication.setNode(n.getUsername());
        replication.setStatus(ReplicationStatus.TRANSFERRED);
        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareACERegister();
        prepareIngestUpdate(ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        AceRunner runner = new AceRunner(ace, generator, replication.getId(), aceConfiguration, bagBucket, tokenBucket, bagOp, tokenOp, notifier);
        runner.get();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(3)).updateStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testAllRunWithCollection() {
        replication = new Replication();
        replication.setBag(b);
        replication.setId(1L);
        replication.setNode(n.getUsername());
        replication.setStatus(ReplicationStatus.TRANSFERRED);
        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareAceGet();
        prepareIngestUpdate(ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        AceRunner runner = new AceRunner(ace, generator, replication.getId(), aceConfiguration, bagBucket, tokenBucket, bagOp, tokenOp, notifier);
        runner.get();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(2)).updateStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testFromTokenLoaded() {
        replication = new Replication();
        replication.setBag(b);
        replication.setId(1L);
        replication.setNode(n.getUsername());
        replication.setStatus(ReplicationStatus.ACE_TOKEN_LOADED);

        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareAceGet();
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        AceRunner runner = new AceRunner(ace, generator, replication.getId(), aceConfiguration, bagBucket, tokenBucket, bagOp, tokenOp, notifier);
        runner.get();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(1)).updateStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testFromRegistered() {
        replication = new Replication();
        replication.setBag(b);
        replication.setId(1L);
        replication.setNode(n.getUsername());
        replication.setStatus(ReplicationStatus.ACE_REGISTERED);

        // setup our mocks for our http requests
        prepareIngestGet(replication.getId(), replication);
        prepareAceGet();
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        notifier = new ReplicationNotifier(replication);

        AceRunner runner = new AceRunner(ace, generator, replication.getId(), aceConfiguration, bagBucket, tokenBucket, bagOp, tokenOp, notifier);
        runner.get();

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong(), eq(false));
        verify(replications, times(2)).updateStatus(anyLong(), any(RStatusUpdate.class));
    }

        private void prepareACERegister() {
        when(bagBucket.fillAceStorage(any(StorageOperation.class), any(GsonCollection.Builder.class)))
                .thenAnswer((Answer<GsonCollection.Builder>) invocation -> invocation.getArgumentAt(1, GsonCollection.Builder.class));
        when(ace.getCollectionByName(any(String.class), any(String.class)))
                .thenReturn(new NotFoundCallWrapper<>());
        when(ace.addCollection(any(GsonCollection.class)))
                .thenReturn(new CallWrapper<>(ImmutableMap.of("id", 1L)));
    }

    // Helper methods for preparing mocks

    private void prepareIngestGet(Long id, Replication r) {
        when(replications.get(id)).thenReturn(new CallWrapper<>(r));
    }

    private void prepareIngestUpdate(ReplicationStatus status) {
        RStatusUpdate update = new RStatusUpdate(status);
        when(replications.updateStatus(anyLong(), eq(update)))
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
     *
     * TODO: We'll want to do this for the NotFoundCW as well
     *
     * @param <E>
     */
    private class AsyncWrapper<E> extends CallWrapper<E> {
        E e;

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

                callback.onResponse(new AsyncWrapper(e), Response.success(e));
            });

            thread.start();
        }
    }

}