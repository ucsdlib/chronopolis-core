package org.chronopolis.replicate.scheduled;

import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;

import java.util.ArrayList;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests for the ReplicationQueryTask
 * The main goal is to make sure we filter properly when checking against the
 * JobExplorer. To do this we mock a few objects (the IngestAPI and JobStarter)
 * in order to ensure our task handles the returns gracefully.
 *
 * In addition we use the @FixMethodOrder annotation because we want to test
 * when a Job is running, but only after we test when no jobs are running. This
 * is used to ensure the order of execution from JUnit.
 *
 * Todo: These tests are somewhat out of date now that {@link Submitter} handles
 * the filtering of replications
 *
 * Created by shake on 3/30/15.
 */
public class ReplicationQueryTaskTest {

    @Mock Submitter submitter;
    @Mock IngestAPI ingestAPI;
    @Mock IngestAPISettings settings;
    @InjectMocks ReplicationQueryTask task;

    private final int NUM_REPLICATIONS = 5;
    private Call<PageImpl<Replication>> replications;


    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.initMocks(this);

        // Init our RQT
        task = new ReplicationQueryTask(settings, ingestAPI, submitter);

        // Init our returned objects
        ArrayList<Replication> replicationList = new ArrayList<>();
        Node n = new Node("test", "test");
        Bag b = new Bag()
                .setName("test-name")
                .setDepositor("test-depositor");
                // .setSize(0L)
                // .setTotalFiles(0L);

        Replication replication = new Replication()
                .setStatus(ReplicationStatus.PENDING)
                .setBag(b)
                .setNode(n.getUsername());
        replication.setId(1L);
        for (int i = 0; i < NUM_REPLICATIONS; i++) {
            replicationList.add(replication);
        }

        PageImpl<Replication> page = new PageImpl<>(replicationList);
        replications = new CallWrapper<>(page);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckForReplications() throws Exception {
        when(ingestAPI.getReplications(anyMap())).thenReturn(replications);
        task.checkForReplications();

        // We have 6 ongoing types of replication states, so query for them
        verify(submitter, times(NUM_REPLICATIONS * 6)).submit(any(Replication.class));
        verify(ingestAPI, times(6)).getReplications(anyMap());
    }

}