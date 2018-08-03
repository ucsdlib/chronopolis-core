package org.chronopolis.replicate.scheduled;

import com.google.common.collect.ImmutableSet;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.kot.api.IngestApiProperties;
import org.chronopolis.rest.kot.api.ReplicationService;
import org.chronopolis.rest.kot.api.ServiceGenerator;
import org.chronopolis.rest.kot.models.Bag;
import org.chronopolis.rest.kot.models.Replication;
import org.chronopolis.rest.kot.models.enums.BagStatus;
import org.chronopolis.rest.kot.models.enums.ReplicationStatus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import retrofit2.Call;

import java.util.ArrayList;

import static java.time.ZonedDateTime.now;
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

    private final int NUM_REPLICATIONS = 5;

    @Mock private Submitter submitter;
    @Mock private ReplicationService replicationService;

    private ReplicationQueryTask task;
    private Call<Iterable<Replication>> replications;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        IngestApiProperties properties = new IngestApiProperties();
        ServiceGenerator generator = new ReplGenerator(replicationService);

        // Init our RQT
        task = new ReplicationQueryTask(properties, generator, submitter);

        // Init our returned objects
        ArrayList<Replication> replicationList = new ArrayList<>();
        // Node n = new Node("test", "test");
        Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(), "test-name", "repl-query-test",
                "test-depositor", BagStatus.REPLICATING, ImmutableSet.of());

        Replication replication = new Replication(1L, now(), now(), ReplicationStatus.PENDING,
                "bag-link", "token-link", "protocol", "", "", "test", bag);
        for (int i = 0; i < NUM_REPLICATIONS; i++) {
            replicationList.add(replication);
        }

        PageImpl<Replication> page = new PageImpl<>(replicationList);
        replications = new CallWrapper<>(page);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckForReplications() {
        when(replicationService.get(anyMap())).thenReturn(replications);
        task.checkForReplications();

        // We have 6 ongoing types of replication states, so query for them
        verify(submitter, times(NUM_REPLICATIONS * 6)).submit(any(Replication.class));
        verify(replicationService, times(6)).get(anyMap());
    }

}