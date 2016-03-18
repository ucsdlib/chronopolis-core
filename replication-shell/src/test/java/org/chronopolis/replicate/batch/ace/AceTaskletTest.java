package org.chronopolis.replicate.batch.ace;

import com.google.common.collect.ImmutableMap;
import okhttp3.RequestBody;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.replicate.ReplicationNotifier;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;

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

    Bag b;
    Node n;
    Field id;
    Replication replication;
    ReplicationNotifier notifier;
    ReplicationSettings settings;

    @Before
    public void setup() throws NoSuchFieldException {
        MockitoAnnotations.initMocks(this);

        b = new Bag("test-bag", "test-depositor");
        b.setTokenLocation("tokens/test-store");
        n = new Node("test-node", "test-node-pass");

        URL bags = ClassLoader.getSystemClassLoader().getResource("preservation");
        settings = new ReplicationSettings();
        settings.setPreservation(bags.toString());

        id = Replication.class.getDeclaredField("id");
        id.setAccessible(true);
    }

    void prepareACERegister() {
        when(ace.addCollection(any(GsonCollection.class)))
                .thenReturn(new CallWrapper<Map<String, Long>>(ImmutableMap.of("id", 1L)));
    }

    void prepareIngestUpdate(ReplicationStatus status) {
        // TODO: Add equals for this shit
        RStatusUpdate update = new RStatusUpdate(status);
        when(ingest.updateReplicationStatus(anyLong(), any(RStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));
    }

    void prepareAceTokenLoad() {
        when(ace.loadTokenStore(anyLong(), any(RequestBody.class)))
                .thenReturn(new CallWrapper<Void>(null));
    }

    void prepareAceAudit() {
        when(ace.startAudit(anyLong()))
                .thenReturn(new CallWrapper<Void>(null));
    }

    void prepareAceGet() {
        GsonCollection collection = new GsonCollection.Builder()
                .name("test-bag")
                .group("test-depositor")
                .state(65)
                .build();
        collection.setId(1L);

        when(ace.getCollectionByName("test-bag", "test-depositor"))
                .thenReturn(new CallWrapper<>(collection));
    }

    @Test
    public void testAllRun() throws Exception {
        replication = new Replication(n, b);
        id.set(replication, 1L);
        notifier = new ReplicationNotifier(replication);
        replication.setStatus(ReplicationStatus.TRANSFERRED);

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
        verify(ace, times(1)).addCollection(any(GsonCollection.class));
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(3)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

    @Test
    public void testFromTokenLoaded() throws Exception {
        replication = new Replication(n, b);
        replication.setStatus(ReplicationStatus.ACE_TOKEN_LOADED);

        id.set(replication, 1L);
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
        replication.setStatus(ReplicationStatus.ACE_REGISTERED);

        // setup our mocks for our http requests
        prepareAceGet();
        prepareAceTokenLoad();
        prepareIngestUpdate(ReplicationStatus.ACE_TOKEN_LOADED);
        prepareAceAudit();
        prepareIngestUpdate(ReplicationStatus.ACE_AUDITING);

        id.set(replication, 1L);
        notifier = new ReplicationNotifier(replication);

        AceTasklet tasklet = new AceTasklet(ingest, ace, replication, settings, notifier);
        tasklet.execute(null, null);

        // Verify our mocks
        verify(ace, times(1)).getCollectionByName("test-bag", "test-depositor");
        verify(ace, times(1)).loadTokenStore(anyLong(), any(RequestBody.class));
        verify(ace, times(1)).startAudit(anyLong());
        verify(ingest, times(2)).updateReplicationStatus(anyLong(), any(RStatusUpdate.class));
    }

}