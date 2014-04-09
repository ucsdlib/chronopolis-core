/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.chronopolis.db;

import javax.persistence.EntityManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import static org.chronopolis.db.JPAAssertions.assertTableExists;
import static org.chronopolis.db.JPAAssertions.assertTableHasColumn;

/**
 *
 * @author shake
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {JPAConfiguration.class})
@Transactional
@TransactionConfiguration(defaultRollback = true)
public class ModelMappingIntegrationTests {
    
    @Autowired
    EntityManager manager;

    @Test
    public void thatItemCustomMappingWorks() throws Exception {
        assertTableExists(manager, "collection_ingest");

        assertTableHasColumn(manager, "collection_ingest", "CORRELATION_ID");
        assertTableHasColumn(manager, "collection_ingest", "TO_DPN");
    }
    
}
