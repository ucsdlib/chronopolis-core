package org.chronopolis.db.intake;

import junit.framework.Assert;
import org.chronopolis.db.JPATestConfiguration;
import org.chronopolis.db.intake.model.Status;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPATestConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class StatusRepositoryTest {

    public static final String BAG_ID = "bagid";
    public static final String DEPOSITOR = "depositor";
    public static final String COLLECTION = "collection_name";

    @Autowired
    StatusRepository repository;


    @Before
    public void setup() {
        Status bagStatus = new Status();
        bagStatus.setBagId(BAG_ID);
        bagStatus.setDepositor(DEPOSITOR);
        bagStatus.setCollectionName(COLLECTION);
        repository.save(bagStatus);
    }

    @Test
    public void testFindByBagId() throws Exception {
        Status bagStatus = repository.findByBagId(BAG_ID);

        Assert.assertEquals(DEPOSITOR, bagStatus.getDepositor());
        Assert.assertEquals(COLLECTION, bagStatus.getCollectionName());

    }
}