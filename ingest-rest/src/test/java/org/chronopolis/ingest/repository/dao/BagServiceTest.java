package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.IngestRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BagServiceTest extends IngestTest {

    private static final String CREATOR = "creator";
    private static final String DEPOSITOR = "test-depositor";

    @Autowired private BagRepository repository;
    @Autowired private EntityManager entityManager;

    private BagService service;

    @Before
    public void setup() {
        service = new BagService(repository, entityManager);
    }

    @Test
    public void processRequestNoRegion() {
        IngestRequest request = new IngestRequest()
                .setName("create-bad-request")
                .setDepositor(DEPOSITOR)
                .setSize(100L)
                .setTotalFiles(5L)
                .setStorageRegion(-1L);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertFalse(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, result.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestNoDepositor() {
        IngestRequest request = new IngestRequest()
                .setName("create-bad-request")
                .setDepositor("invalid-depositor")
                .setSize(100L)
                .setTotalFiles(5L)
                .setStorageRegion(1L);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertFalse(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.BAD_REQUEST, result.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestConflict() {
        IngestRequest request = new IngestRequest()
                .setName("create-conflict")
                .setDepositor(DEPOSITOR)
                .setSize(100L)
                .setTotalFiles(5L)
                .setStorageRegion(1L);
        service.processRequest(CREATOR, request);
        BagCreateResult conflict = service.processRequest(CREATOR, request);
        Assert.assertFalse(conflict.getBag().isPresent());
        Assert.assertEquals(HttpStatus.CONFLICT, conflict.getResponseEntity().getStatusCode());
    }

    @Test
    public void processRequestSuccess() {
        IngestRequest request = new IngestRequest()
                .setName("create-success")
                .setDepositor(DEPOSITOR)
                .setSize(100L)
                .setTotalFiles(5L)
                .setStorageRegion(1L);
        BagCreateResult result = service.processRequest(CREATOR, request);
        Assert.assertTrue(result.getBag().isPresent());
        Assert.assertEquals(HttpStatus.CREATED, result.getResponseEntity().getStatusCode());

        Bag created = result.getBag().get();
        Bag fromDb = service.find(new BagSearchCriteria().withId(created.getId()));
        Assert.assertEquals(3, fromDb.getDistributions().size());
    }

}