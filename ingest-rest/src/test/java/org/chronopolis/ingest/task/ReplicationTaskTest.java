package org.chronopolis.ingest.task;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.ReplicationDao;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.entities.Replication;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;
import java.util.List;

import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

/**
 * Tests to validate replications are created properly
 *
 * Created by shake on 8/6/15.
 */
@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class ReplicationTaskTest extends IngestTest {

    private ReplicationDao dao;
    private ReplicationTask task;

    @Autowired EntityManager manager;

    @Before
    public void setup() {
        dao = new ReplicationDao(manager);
        task = new ReplicationTask(dao);
    }

    @Test
    public void testCreateReplications() {
        task.createReplications();

        // Based on the sql we should have 4 replications
        List<Replication> all = dao.findAll(QReplication.replication);
        assertEquals(4, all.size());
    }

}