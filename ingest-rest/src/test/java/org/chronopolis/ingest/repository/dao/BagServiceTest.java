package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.support.BagCreateResult;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.PartialBag;
import org.chronopolis.rest.models.create.BagCreate;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.List;

import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

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

    @Autowired
    private EntityManager entityManager;

    private BagDao service;

    @Before
    public void setup() {
        service = new BagDao(entityManager);
    }


    private final Logger log = LoggerFactory.getLogger(BagServiceTest.class);

    @Test
    @SqlGroup({
            @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
            @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
    })
    public void bagPartialProjection() {
        List<PartialBag> partialBags = service.partialViews(new BagFilter());
        log.info("fetched size: {}", partialBags.size());
        partialBags.forEach(partial -> log.info("{}", partial));
        Assert.assertEquals(10, partialBags.size());
    }

    @Test
    @SqlGroup({
            @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
            @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
    })
    public void bagCompleteProjection() {
        String name = "bag-3";
        Bag one = service.findOne(QBag.bag, QBag.bag.name.eq(name));
        CompleteBag view = service.findCompleteView(one.getId());

        Assert.assertNotNull(view);
        Assert.assertNotNull(view.getStorage());
        Assert.assertEquals(1, view.getStorage().size());
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