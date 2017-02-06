package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.RepairSearchCriteria;
import org.chronopolis.ingest.repository.RepairService;
import org.chronopolis.ingest.repository.SearchService;
import org.chronopolis.rest.entities.Fulfillment;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.models.repair.ACEStrategy;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.data.domain.Page;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.security.Principal;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.Assert.assertEquals;

/**
 * Tests for our repair controller
 *
 * I'm not sure if this is the best way to test our controller (calling it directly),
 * but it seems pretty straightforward.
 *
 * Also it would be nice if we could programmatically generate test cases instead of
 * using a static set from the sql. It would help in that we could have less hard coded
 * values (node/principal names, ids) which probably don't make sense when first reading
 * the tests.
 *
 * Created by shake on 1/26/17.
 */
@WebIntegrationTest("server.port:0")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createRepairs.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteRepairs.sql")
})
public class RepairControllerTest extends IngestTest {
    private final Logger log = LoggerFactory.getLogger(RepairControllerTest.class);

    @Autowired private RepairController controller;
    @Autowired private RepairService<Repair, Long, RepairRepository> repairs;
    @Autowired private SearchService<Fulfillment, Long, FulfillmentRepository> fulfillments;

    @Test
    public void getRepair() throws Exception {
        Repair repair = controller.getRequest(1L);
        assertEquals(RepairStatus.FULFILLING, repair.getStatus());
        assertEquals("ucsd", repair.getRequester());
    }

    @Test
    public void getRepairs() throws Exception {
        Page<Repair> repairs = controller.getRequests(ImmutableMap.of());
        assertEquals(3, repairs.getTotalElements());
    }

    @Test
    public void getFulfillment() {
        Fulfillment fulfillment = controller.getFulfillment(1L);
        assertEquals("umiacs", fulfillment.getFrom().getUsername());
        assertEquals(FulfillmentStatus.STAGING, fulfillment.getStatus());
    }

    @Test
    public void getFulfillments() {
        Page<Fulfillment> fulfillments = controller.getFulfillments(ImmutableMap.of());
        assertEquals(2, fulfillments.getTotalElements());
    }


    @Test
    @WithMockUser("umiacs")
    public void createRepair() throws Exception {
        RepairRequest request = new RepairRequest()
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        Repair repair = controller.createRequest(mockPrincipal("umiacs"), request);
        assertEquals(RepairStatus.REQUESTED, repair.getStatus());
        assertEquals("umiacs", repair.getRequester());
        assertEquals("umiacs", repair.getTo().getUsername());
    }

    @Test
    @WithMockUser(value = "admin", roles = "ADMIN")
    public void createRepairAdmin() throws Exception {
        RepairRequest request = new RepairRequest()
                .setTo("umiacs")
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        Repair repair = controller.createRequest(mockPrincipal("admin"), request);
        assertEquals(RepairStatus.REQUESTED, repair.getStatus());
        assertEquals("admin", repair.getRequester());
        assertEquals("umiacs", repair.getTo().getUsername());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser("ncar")
    public void createRepairUnauthorized() throws Exception {
        RepairRequest request = new RepairRequest()
                .setTo("umiacs")
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        controller.createRequest(mockPrincipal("ncar"), request);
    }

    @Test
    @WithMockUser("umiacs")
    public void fulfillRequest() throws Exception {
        controller.fulfillRequest(mockPrincipal("umiacs"), 2L);
        Repair repair = repairs.find(new RepairSearchCriteria().withId(2L));
        assertNotNull(repair.getFulfillment());
        assertEquals("umiacs", repair.getFulfillment().getFrom().getUsername());
    }

    @WithMockUser("ncar")
    @Test(expected = BadRequestException.class)
    public void fulfillOwnRequest() throws Exception {
        controller.fulfillRequest(mockPrincipal("ncar"), 2L);
    }

    /*
    // Not implemented yet
    @Test
    public void fulfillAlreadyExists() throws Exception {
        log.info(entity.toString());
        assertEquals(HttpStatus.BAD_REQUEST, entity.getStatusCode());
    }
    */

    @Test
    @WithMockUser(username = "umiacs")
    public void readyFulfillment() throws Exception {
        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        Fulfillment entity = controller.readyFulfillment(mockPrincipal("umiacs"), strategy, 1L);

        log.info(entity.toString());
        assertEquals(FulfillmentType.ACE, entity.getType());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(username = "umiacs")
    public void readyFulfillmentUnauthorized() throws Exception {
        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        controller.readyFulfillment(mockPrincipal("umiacs"), strategy, 2L);
    }

    @Test
    @WithMockUser(username = "ucsd")
    public void completeFulfillment() throws Exception {
        Fulfillment fulfillment = controller.completeFulfillment(mockPrincipal("ucsd"), 1L);
        Repair repair = fulfillment.getRepair();

        assertEquals(FulfillmentStatus.COMPLETE, fulfillment.getStatus());
        assertEquals(RepairStatus.REPAIRED, repair.getStatus());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(username = "umiacs")
    public void completeFulfillmentUnauthorized() throws Exception {
        controller.completeFulfillment(mockPrincipal("umiacs"), 1L);
    }

    private Principal mockPrincipal(String name) {
        return () -> name;
    }
}