package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableMap;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.NotFoundCallWrapper;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Callback;
import retrofit2.Response;

import java.net.URL;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 3/18/16.
 */
public class AceTaskletTest {

    final String name = "test-bag";
    final String group = "test-depositor";

    @Mock IngestAPI ingest;
    @Mock AceService ace;

    private Bag b;
    private Node n;
    private Replication replication;
    private ReplicationNotifier notifier;
    private ReplicationSettings settings;

    @Before
    public void setup() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);

        b = new Bag("test-bag", "test-depositor");
        b.setTokenLocation("tokens/test-store");
        n = new Node("test-node", "test-node-pass");

        URL bags = ClassLoader.getSystemClassLoader().getResource("preservation");
        settings = new ReplicationSettings();
        settings.setPreservation(bags.toString());
    }

    private void prepareACERegister() {
        when(ace.getCollectionByName(any(String.class), any(String.class)))
                .thenReturn(new NotFoundCallWrapper<>());
        when(ace.addCollection(any(GsonCollection.class)))
                .thenReturn(new CallWrapper<>(ImmutableMap.of("id", 1L)));
    }

    private void prepareIngestUpdate(ReplicationStatus status) {
        // TODO: Add equals for this
        RStatusUpdate update = new RStatusUpdate(status);
        when(ingest.updateReplicationStatus(anyLong(), any(RStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));
    }

    private void prepareAceTokenLoad() {
        when(ace.loadTokenStore(anyLong(), any(RequestBody.class)))
                .thenReturn(new CallWrapper<>(null));
    }

    private void prepareAceAudit() {
        when(ace.startAudit(anyLong()))
                .thenReturn(new CallWrapper<>(null));
    }

    private void prepareAceGet() {
        GsonCollection collection = new GsonCollection.Builder()
                .name("test-bag")
                .group("test-depositor")
                .state(65)
                .build();
        collection.setId(1L);

        when(ace.getCollectionByName("test-bag", "test-depositor"))
                .thenReturn(new AsyncWrapper<>(collection));
    }

    @Test
    public void testAllRun() throws Exception {
        replication = new Replication(n, b);
        replication.setId(1L);
        replication.setStatus(ReplicationStatus.TRANSFERRED);
        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareACERegister();
        prepareIngestUpdate(ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        // Luckily we don't use either parameter passed in, so fuck em
        AceTasklet tasklet = new AceTasklet(ingest, ace, replication, settings, notifier);
        tasklet.execute(null, null);

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(3)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testAllRunWithCollection() throws Exception {
        replication = new Replication(n, b);
        replication.setId(1L);
        replication.setStatus(ReplicationStatus.TRANSFERRED);
        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareAceGet();
        prepareIngestUpdate(ReplicationStatus.ACE_REGISTERED);
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        // Luckily we don't use either parameter passed in, so fuck em
        AceTasklet tasklet = new AceTasklet(ingest, ace, replication, settings, notifier);
        tasklet.execute(null, null);

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName(any(String.class), any(String.class));
        verify(ace, times(0)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(2)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testFromTokenLoaded() throws Exception {
        replication = new Replication(n, b);
        replication.setId(1L);
        replication.setStatus(ReplicationStatus.ACE_TOKEN_LOADED);

        notifier = new ReplicationNotifier(replication);

        // setup our mocks for our http requests
        prepareAceGet();
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        AceTasklet tasklet = new AceTasklet(ingest, ace, replication, settings, notifier);
        tasklet.execute(null, null);

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(1)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testFromRegistered() throws Exception {
        replication = new Replication(n, b);
        replication.setId(1L);
        replication.setStatus(ReplicationStatus.ACE_REGISTERED);

        // setup our mocks for our http requests
        prepareAceGet();
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        notifier = new ReplicationNotifier(replication);

        AceTasklet tasklet = new AceTasklet(ingest, ace, replication, settings, notifier);
        tasklet.execute(null, null);

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(2)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

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
            Thread thread = new Thread() {
                public void run() {
                    try {
                        TimeUnit.SECONDS.sleep(2);
                    } catch (InterruptedException ignored) {
                    }
                    callback.onResponse(Response.success(e));
                }
            };

            thread.start();
        }
    }

}