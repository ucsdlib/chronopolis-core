package org.chronopolis.db.ingest;

import junit.framework.Assert;
import org.chronopolis.db.JPATestConfiguration;
import org.chronopolis.db.model.ReplicationFlow;
import org.chronopolis.db.model.ReplicationState;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.Assert.*;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPATestConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class ReplicationFlowTableTest {

    @Autowired ReplicationFlowTable table;

    final String collection = "collection";
    final String depositor = "depositor";
    final ReplicationState state = ReplicationState.INIT;
    final String node = "node";

    @Before
    public void setup() {
        ReplicationFlow item = new ReplicationFlow();
        item.setCollection(collection);
        item.setCurrentState(state);
        item.setDepositor(depositor);
        item.setNode(node);

        table.save(item);
    }


    @Test
    public void testFindByDepositorAndCollection() throws Exception {
        ReplicationFlow item = table.findByDepositorAndCollectionAndNode(depositor,
                collection,
                node);

        Assert.assertNotNull(item);
        Assert.assertEquals(state, item.getCurrentState());

    }
}