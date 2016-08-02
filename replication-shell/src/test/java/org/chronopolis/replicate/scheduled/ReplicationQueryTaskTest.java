package org.chronopolis.replicate.scheduled;

import com.google.common.collect.ImmutableMap;
import org.chronopolis.common.settings.IngestAPISettings;
import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.replicate.support.CallWrapper;
import org.chronopolis.replicate.test.TestApplication;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.chronopolis.rest.support.BagConverter;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import retrofit2.Call;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
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
 * Created by shake on 3/30/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@SpringApplicationConfiguration(classes = TestApplication.class)
public class ReplicationQueryTaskTest {
    private final Logger log = LoggerFactory.getLogger(ReplicationQueryTaskTest.class);

    @InjectMocks
    ReplicationQueryTask task;

    @Mock
    IngestAPI ingestAPI;

    @Mock
    IngestAPISettings settings;

    @Mock
    ReplicationJobStarter jobStarter;

    @Autowired
    JobExplorer jobExplorer;

    @Autowired
    JobLauncher launcher;

    @Autowired
    Job job;

    Bag b;
    Replication replication;
    Map<String, Object> started;
    Map<String, Object> pending;

    Call<org.chronopolis.rest.models.Bag> bagCall;
    Call<PageImpl<Replication>> replications;


    @Before
    public void init() throws NoSuchFieldException, IllegalAccessException {
        log.info("Creating mocks");
        MockitoAnnotations.initMocks(this);

        // Make sure the autowired JobExplorer gets used
        Field f = task.getClass().getDeclaredField("explorer");
        f.setAccessible(true);
        f.set(task, jobExplorer);

        ArrayList<Replication> replicationList = new ArrayList<>();
        Node n = new Node("test", "test");
        b = new Bag("test-bag", "test-depositor");
        b.setSize(0);
        b.setTotalFiles(0);

        replication = new Replication(n, b);
        replication.setId(1L);
        for (int i = 0; i < 5; i++) {
            replicationList.add(replication);
        }

        PageImpl<Replication> page = new PageImpl<>(replicationList);

        replications = new CallWrapper<>(page);
        bagCall = new CallWrapper<>(BagConverter.toBagModel(b));

        started = ImmutableMap.of("status", ReplicationStatus.STARTED);
        pending = ImmutableMap.of("status", ReplicationStatus.PENDING);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckForReplications() throws Exception {
        when(ingestAPI.getReplications(anyMap())).thenReturn(replications);

        // Ok so this is kind of bad behavior, but our bag has a null id so...
        when(ingestAPI.getBag(b.getId())).thenReturn(bagCall);
        when(ingestAPI.updateReplicationStatus(anyLong(), any(RStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));
        task.checkForReplications();

        // We should have only executed our job starter once
        verify(jobStarter).addJobFromRestful(replication);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCheckForReplicationsFilter() throws Exception {
        // Create a job that only sleeps in the same way we build our replication
        // jobs to make sure we filter properly when querying the job explorer
        launcher.run(job, new JobParametersBuilder()
            .addString("depositor", replication.getBag().getDepositor())
            .addString("collection", replication.getBag().getName())
            .addDate("date", new Date())
            .toJobParameters());
        when(ingestAPI.getReplications(anyMap())).thenReturn(replications);
        when(ingestAPI.getBag(b.getId())).thenReturn(bagCall);
        when(ingestAPI.updateReplicationStatus(anyLong(), any(RStatusUpdate.class)))
                .thenReturn(new CallWrapper<>(replication));

        task.checkForReplications();
        verify(jobStarter, times(0)).addJobFromRestful(replication);

    }

}