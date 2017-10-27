package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus.REPLICATE;

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
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBagsWithReplications.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBagsWithReplications.sql")
})
public class ReplicatingNodeTest extends IngestTest {

    @Autowired NodeRepository nodeRepository;
    @Autowired BagRepository bagRepository;

    @Test
    public void testNumReplications() {
        Bag bag = bagRepository.findOne(new Long(1));
        Assert.assertEquals(2, bag.getReplicatingNodes().size());
    }

    @Test
    public void testUpdateReplications() {
        // Add replicating nodes
        Bag bag = bagRepository.findOne(new Long(2));
        for (Node node : nodeRepository.findAll()) {
            bag.addDistribution(node, REPLICATE);
        }
        bagRepository.save(bag);

        // And test that we pulled them all
        Bag updated = bagRepository.findOne(new Long(2));
        Assert.assertEquals(4, updated.getReplicatingNodes().size());
    }

}
