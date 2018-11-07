package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.create.BagCreate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BagServiceTest extends IngestTest {

    private static final String CREATOR = "creator";
    private static final String LOCATION = "test-location";
    private static final String DEPOSITOR = "test-depositor";
    private static final String BAD_DEPOSITOR = "invalid-depositor";
    private static final String BAG_SUCCESS = "create-success";
    private static final String BAG_CONFLICT = "create-conflict";
    private static final String BAG_BAD_REQUEST = "create-bad-request";

    @Autowired private EntityManager entityManager;

    private BagDao service;

    @Before
    public void setup() {
        service = new BagDao(entityManager);
    }

    @Test
    @Ignore("no longer part of the dao")
    public void processRequestNoRegion() {
        BagCreate request = new BagCreate(BAG_BAD_REQUEST, 100L, 5L, -1L, LOCATION, DEPOSITOR);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertFalse(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, result.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestNoDepositor() {
        BagCreate request = new BagCreate(BAG_BAD_REQUEST, 100L, 5L, 1L, LOCATION, BAD_DEPOSITOR);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertFalse(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, result.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestConflict() {
        BagCreate request = new BagCreate(BAG_CONFLICT, 100L, 5L, 1L, LOCATION, DEPOSITOR);
        service.processRequest(CREATOR, request);
        BagCreateResult conflict = service.processRequest(CREATOR, request);
        Assert.assertFalse(conflict.getBag().isPresent());
        Assert.assertEquals(HttpStatus.CONFLICT, conflict.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestSuccess() {
        BagCreate request = new BagCreate(BAG_SUCCESS, 100L, 5L, 1L, LOCATION, DEPOSITOR);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertTrue(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.CREATED, result.getResponseEntity().getStatusCode());

        Bag created = result.getBag().get();
        Bag fromDb = service.findOne(QBag.bag, QBag.bag.id.eq(created.getId()));
        Assert.assertEquals(3, fromDb.getDistributions().size());
    }

}