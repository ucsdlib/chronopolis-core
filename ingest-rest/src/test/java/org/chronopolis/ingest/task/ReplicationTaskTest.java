package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.rest.models.Replication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests to validate replications are created properly
 *
 * Created by shake on 8/6/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        // We only want bags to be inserted for these tests
        // but when tearing down remove the replications as well
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBags.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteReplications.sql")
})
public class ReplicationTaskTest extends IngestTest {

    @Autowired
    ReplicationTask task;

    @Autowired
    ReplicationRepository repository;

    @Test
    public void testCreateReplications() throws Exception {
        task.createReplications();

        // Based on the sql we should only have 3 replications
        // (only 3 bag_distribution records created)
        List<Replication> all = repository.findAll();
        assertEquals(3, all.size());
    }

}