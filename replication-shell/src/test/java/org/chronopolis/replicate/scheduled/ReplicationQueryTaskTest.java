package org.chronopolis.replicate.scheduled;

import com.google.common.collect.ImmutableSet;
import org.chronopolis.replicate.batch.Submitter;
import org.chronopolis.replicate.support.ReplGenerator;
import org.chronopolis.rest.api.IngestApiProperties;
import org.chronopolis.rest.api.ReplicationService;
import org.chronopolis.rest.api.ServiceGenerator;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.page.SpringPage;
import org.chronopolis.rest.models.page.SpringPageKt;
import org.chronopolis.test.support.CallWrapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;

import java.util.ArrayList;

import static java.time.ZonedDateTime.now;
import static org.mockito.ArgumentMatchers.any;
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
    private Call<SpringPage<Replication>> replications;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        IngestApiProperties properties = new IngestApiProperties();
        ServiceGenerator generator = new ReplGenerator(replicationService);

        // Init our RQT
        task = new ReplicationQueryTask(properties, generator, submitter);

        // Init our returned objects
        ArrayList<Replication> replicationList = new ArrayList<>();
        Bag bag = new Bag(1L, 1L, 1L, null, null, now(), now(), "test-name", "repl-query-test",
                "test-depositor", BagStatus.REPLICATING, ImmutableSet.of());

        Replication replication = new Replication(1L, now(), now(), ReplicationStatus.PENDING,
                "bag-link", "token-link", "protocol", "", "", "test", bag);
        for (int i = 0; i < NUM_REPLICATIONS; i++) {
            replicationList.add(replication);
        }

        replications = new CallWrapper<>(SpringPageKt.wrap(replicationList));
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