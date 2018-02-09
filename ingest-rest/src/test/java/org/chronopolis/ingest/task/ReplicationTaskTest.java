package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.ReplicationRepository;
import org.chronopolis.ingest.repository.dao.ReplicationService;
import org.chronopolis.rest.entities.Replication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import java.util.List;

import static junit.framework.Assert.assertEquals;

/**
 * Tests to validate replications are created properly
 *
 * Created by shake on 8/6/15.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        // We only want bags to be inserted for these tests
        // but when tearing down remove the replications as well
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBags.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteReplications.sql")
})
public class ReplicationTaskTest extends IngestTest {

    private ReplicationTask task;

    @Autowired EntityManager manager;
    @Autowired ReplicationRepository repository;
    @Autowired BagRepository bags;
    @Autowired NodeRepository nodes;

    @Before
    public void setup() {
        ReplicationService service = new ReplicationService(manager, repository,  bags, nodes);
        task = new ReplicationTask(bags, service);
    }

    @Test
    public void testCreateReplications() throws Exception {
        task.createReplications();

        // Based on the sql we should have 4 replications
        List<Replication> all = repository.findAll();
        assertEquals(4, all.size());
    }

}