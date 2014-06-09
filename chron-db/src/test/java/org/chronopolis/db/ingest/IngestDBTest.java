package org.chronopolis.db.ingest;

import org.chronopolis.db.JPATestConfiguration;
import org.chronopolis.db.model.CollectionIngest;
import org.junit.Assert;
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
public class IngestDBTest {

    @Autowired
    IngestDB ingestDB;

    CollectionIngest ci;

    final String correlationId = "correlation-id";

    @Before
    public void startup() {
        ci = new CollectionIngest();
        ci.setCorrelationId(correlationId);
        ci.setToDpn(false);

        ingestDB.save(ci);
    }

    @Test
    public void testFindByCorrelationId() throws Exception {
        CollectionIngest ci = ingestDB.findByCorrelationId(correlationId);

        Assert.assertNotNull(ci);
        Assert.assertFalse(ci.getToDpn());
    }
}