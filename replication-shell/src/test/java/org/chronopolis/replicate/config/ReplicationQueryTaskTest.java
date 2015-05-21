package org.chronopolis.replicate.config;

import org.chronopolis.replicate.batch.ReplicationJobStarter;
import org.chronopolis.replicate.test.TestApplication;
import org.chronopolis.rest.api.IngestAPI;
import org.chronopolis.rest.models.Bag;
import org.chronopolis.rest.models.Node;
import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.SimpleJob;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


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
    ReplicationJobStarter jobStarter;

    @Autowired
    JobExplorer jobExplorer;

    @Autowired
    JobLauncher launcher;

    @Autowired
    Job job;

    Page<Replication> replications;

    Replication replication;

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
        Bag b = new Bag("test-bag", "test-depositor");
        replication = new Replication(n, b);
        for (int i = 0; i < 5; i++) {
            replicationList.add(replication);
        }
        replications = new PageImpl<>(replicationList);
    }

    @Test
    public void testCheckForReplications() throws Exception {
        Mockito.when(ingestAPI.getReplications(ReplicationStatus.STARTED)).thenReturn(replications);
        Mockito.when(ingestAPI.getReplications(ReplicationStatus.PENDING)).thenReturn(replications);
        task.checkForReplications();

        // We should have only executed our job starter once
        Mockito.verify(jobStarter).addJobFromRestful(replication);
    }

    @Test
    public void testCheckForReplicationsFilter() throws Exception {
        // Create a job that only sleeps in the same way we build our replication
        // jobs to make sure we filter properly when querying the job explorer
        launcher.run(job, new JobParametersBuilder()
            .addString("depositor", replication.getBag().getDepositor())
            .addString("collection", replication.getBag().getName())
            .addDate("date", new Date())
            .toJobParameters());
        Mockito.when(ingestAPI.getReplications(ReplicationStatus.STARTED)).thenReturn(replications);
        Mockito.when(ingestAPI.getReplications(ReplicationStatus.PENDING)).thenReturn(replications);
        task.checkForReplications();
        Mockito.verify(jobStarter, Mockito.times(0)).addJobFromRestful(replication);

    }
}