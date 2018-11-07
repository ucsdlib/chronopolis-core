package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QNode;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;


/**
 * Test for the ManyToMany relationship between bags and nodes. One is a simple
 * query for a bag which already has replications, and the other is adding them
 * and testing they persist.
 *
 * Created by shake on 4/14/15.
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = "classpath:sql/create.sql"),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = "classpath:sql/delete.sql")
})
public class ReplicatingNodeTest extends IngestTest {

    @Autowired EntityManager entityManager;

    private PagedDao dao;
    private final String DEPOSITOR = "test-depositor";

    @Before
    public void setup() {
        dao = new PagedDao(entityManager);
    }

    @Test
    public void testNumReplications() {
        String name = "bag-0";
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(name));
        Assert.assertEquals(2, bag.getReplicatingNodes().size());
    }

    @Test
    public void testUpdateReplications() {
        String name = "bag-1";
        // Add replicating nodes
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(name));
        for (Node node : dao.findAll(QNode.node)) {
            bag.addDistribution(node, BagDistributionStatus.REPLICATE);
        }
        dao.save(bag);

        // And test that we pulled them all
        Bag updated = dao.findOne(QBag.bag, QBag.bag.name.eq(name));
        Assert.assertEquals(4, updated.getReplicatingNodes().size());
    }

}
