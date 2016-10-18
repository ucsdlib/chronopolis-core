package org.chronopolis.replicate.batch;

import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.concurrent.TrackingThreadPoolExecutor;
import org.chronopolis.replicate.config.ReplicationSettings;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.time.ZonedDateTime;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 *
 * Created by shake on 10/18/16.
 */
public class SubmitterTest {

    Submitter submitter;
    ReplicationSettings settings;

    // Mock these? No... that wouldn't be good...
    TrackingThreadPoolExecutor<Replication> io;
    TrackingThreadPoolExecutor<Replication> http;

    @Mock AceService ace;
    @Mock IngestAPI ingest;

    Node node;
    Bag bag;


    @Before
    public void setup() {
        ace = mock(AceService.class);
        ingest = mock(IngestAPI.class);
        settings = new ReplicationSettings();
        io = new TrackingThreadPoolExecutor<>(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        http = new TrackingThreadPoolExecutor<>(1, 1, 1, TimeUnit.SECONDS, new LinkedBlockingDeque<>());
        submitter = new Submitter(ace, ingest, settings, io, http);

        node = new Node("node-user", "not-a-real-field");
        bag = new Bag("test-bag", "test-depositor")
                .setCreator("submitter-test");
        bag.setId(1L);
        bag.setTokenLocation("test-token-location");
        bag.setLocation("test-bag-location");
        bag.setCreatedAt(ZonedDateTime.now());
        bag.setUpdatedAt(ZonedDateTime.now());

    }

    @Test
    public void testAceCheck() {
        Replication r = new Replication(node, bag);
        r.setId(1L);
        r.setCreatedAt(ZonedDateTime.now());
        r.setUpdatedAt(ZonedDateTime.now());
        r.setBagId(1L);
        r.setNodeUser(node.username);
        r.setProtocol("rsync");
        r.setStatus(ReplicationStatus.ACE_AUDITING);
        GsonCollection c = new GsonCollection.Builder()
                .name(bag.getName())
                .group(bag.getDepositor())
                .state(65)
                .storage("local")
                .build();

        when(ace.getCollectionByName(bag.getName(), bag.getDepositor())).thenReturn(new CallWrapper<>(c));
        submitter.submit(r);

        verify(ace, times(1)).getCollectionByName(bag.getName(), bag.getDepositor());
    }

}