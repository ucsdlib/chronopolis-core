package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.exception.BadRequestException;
import org.chronopolis.ingest.exception.ConflictException;
import org.chronopolis.ingest.exception.NotFoundException;
import org.chronopolis.ingest.exception.UnauthorizedException;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.RepairSearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Fulfillment;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.models.repair.ACEStrategy;
import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;
import org.chronopolis.rest.models.repair.RepairRequest;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.junit.Assert;
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

    private static final String NCAR = "ncar";
    private static final String UCSD = "ucsd";
    private static final String ADMIN = "admin";
    private static final String UMIACS = "umiacs";
    private static Principal ncarPrincipal = () -> NCAR;
    private static Principal ucsdPrincipal = () -> UCSD;
    private static Principal adminPrincipal = () -> ADMIN;
    private static Principal umiacsPrincipal = () -> UMIACS;

    @Autowired private RepairController controller;
    @Autowired private SearchService<Repair, Long, RepairRepository> repairs;
    @Autowired private SearchService<Fulfillment, Long, FulfillmentRepository> fulfillments;

    @Test
    public void getRepair() throws Exception {
        Repair repair = controller.getRequest(1L);
        assertEquals(RepairStatus.FULFILLING, repair.getStatus());
        assertEquals(UCSD, repair.getRequester());
    }

    @Test(expected = NotFoundException.class)
    public void getRepairNotFound() {
        controller.getRequest(100L);
    }

    @Test
    public void getRepairs() throws Exception {
        Page<Repair> repairs = controller.getRequests(ImmutableMap.of());
        assertEquals(3, repairs.getTotalElements());
    }

    @Test
    public void getFulfillment() {
        Fulfillment fulfillment = controller.getFulfillment(1L);
        assertEquals(UMIACS, fulfillment.getFrom().getUsername());
        assertEquals(FulfillmentStatus.STAGING, fulfillment.getStatus());
    }

    @Test(expected = NotFoundException.class)
    public void getFulfillmentNotFound() {
        controller.getFulfillment(100L);
    }

    @Test
    public void getFulfillments() {
        Page<Fulfillment> fulfillments = controller.getFulfillments(ImmutableMap.of());
        assertEquals(2, fulfillments.getTotalElements());
    }


    @Test
    @WithMockUser(UMIACS)
    public void createRepair() throws Exception {
        RepairRequest request = new RepairRequest()
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        Repair repair = controller.createRequest(umiacsPrincipal, request);
        assertEquals(RepairStatus.REQUESTED, repair.getStatus());
        assertEquals(UMIACS, repair.getRequester());
        assertEquals(UMIACS, repair.getTo().getUsername());
    }

    @Test
    @WithMockUser(value = ADMIN, roles = ADMIN)
    public void createRepairAdmin() throws Exception {
        RepairRequest request = new RepairRequest()
                .setTo(UMIACS)
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        Repair repair = controller.createRequest(adminPrincipal, request);
        assertEquals(RepairStatus.REQUESTED, repair.getStatus());
        assertEquals(ADMIN, repair.getRequester());
        assertEquals(UMIACS, repair.getTo().getUsername());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void createRepairUnauthorized() throws Exception {
        RepairRequest request = new RepairRequest()
                .setTo(UMIACS)
                .setCollection("bag-0")
                .setDepositor("test-depositor")
                .setFiles(ImmutableSet.of("test-file-1"));

        controller.createRequest(ncarPrincipal, request);
    }

    @Test
    @WithMockUser(UMIACS)
    public void fulfillRequest() throws Exception {
        controller.fulfillRequest(umiacsPrincipal, 2L);
        Repair repair = repairs.find(new RepairSearchCriteria().withId(2L));
        assertNotNull(repair.getFulfillment());
        assertEquals(UMIACS, repair.getFulfillment().getFrom().getUsername());
    }

    @Test(expected = ConflictException.class)
    @WithMockUser(NCAR)
    public void fulfillRequestConflict() {
        controller.fulfillRequest(ncarPrincipal, 1L);
    }

    @WithMockUser(NCAR)
    @Test(expected = BadRequestException.class)
    public void fulfillOwnRequest() throws Exception {
        controller.fulfillRequest(ncarPrincipal, 2L);
    }

    @Test
    @WithMockUser(UMIACS)
    public void readyFulfillment() throws Exception {
        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        Fulfillment entity = controller.readyFulfillment(umiacsPrincipal, strategy, 1L);

        log.info(entity.toString());
        assertEquals(FulfillmentType.ACE, entity.getType());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void readyFulfillmentUnauthorized() throws Exception {
        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        controller.readyFulfillment(ncarPrincipal, strategy, 1L);
    }

    @Test(expected = BadRequestException.class)
    @WithMockUser(UCSD)
    public void completeFulfillmentNoStrategy() throws Exception {
        controller.completeFulfillment(ucsdPrincipal, 1L);
    }


    @Test
    @WithMockUser(UMIACS)
    public void completeFulfillment() throws Exception {
        Fulfillment fulfillment = controller.completeFulfillment(umiacsPrincipal, 2L);
        Repair repair = fulfillment.getRepair();

        assertEquals(FulfillmentStatus.COMPLETE, fulfillment.getStatus());
        assertEquals(RepairStatus.REPAIRED, repair.getStatus());
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void completeFulfillmentUnauthorized() throws Exception {
        controller.completeFulfillment(ncarPrincipal, 2L);
    }

    @Test
    @WithMockUser(UCSD)
    public void repairAuditing() {
        AuditStatus auditing = AuditStatus.AUDITING;
        Repair repair = controller.repairAuditing(ucsdPrincipal, 1L, auditing);
        Assert.assertEquals(auditing, repair.getAudit());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(UCSD)
    public void repairAuditingNotFound() {
        controller.repairAuditing(ucsdPrincipal, 14L, AuditStatus.AUDITING);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(username = "umiacs")
    public void repairAuditingUnauthorized() {
        controller.repairAuditing(umiacsPrincipal, 1L, AuditStatus.AUDITING);
    }

    @Test
    @WithMockUser(UCSD)
    public void repairCleaned() {
        Repair repair = controller.repairCleaned(ucsdPrincipal, 1L);
        Assert.assertEquals(true, repair.getCleaned());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(UCSD)
    public void repairCleanNotFound() {
        controller.repairCleaned(ucsdPrincipal, 12L);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(UMIACS)
    public void repairCleanUnauthorized() {
        controller.repairCleaned(umiacsPrincipal, 1L);
    }

    @Test
    @WithMockUser(UCSD)
    public void repairReplaced() {
        Repair repair = controller.repairReplaced(ucsdPrincipal, 1L);
        Assert.assertEquals(true, repair.getReplaced());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(UCSD)
    public void repairReplacedNotFound() {
        controller.repairReplaced(ucsdPrincipal, 13L);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(UMIACS)
    public void repairReplacedUnauthorized() {
        controller.repairReplaced(umiacsPrincipal, 1L);
    }

    @Test
    @WithMockUser(UMIACS)
    public void fulfillmentCleaned() {
        Fulfillment fulfillment = controller.fulfillmentCleaned(umiacsPrincipal, 1L);
        Assert.assertEquals(true, fulfillment.getCleaned());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(UMIACS)
    public void fulfillmentCleanNotFound() {
        controller.fulfillmentCleaned(umiacsPrincipal, 12L);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void fulfillmentCleanUnauthorized() {
        controller.fulfillmentCleaned(ncarPrincipal, 1L);
    }

    @Test
    @WithMockUser(UMIACS)
    public void fulfillmentUpdated() {
        // The likelihood of this test happen is very low, but we're doing it just for completeness
        FulfillmentStatus ready = FulfillmentStatus.READY;
        Fulfillment fulfillment = controller.fulfillmentUpdated(umiacsPrincipal, 1L, ready);
        Assert.assertEquals(ready, fulfillment.getStatus());
    }

    @Test
    @WithMockUser(UCSD)
    public void fulfillmentUpdatedXfer() {
        FulfillmentStatus transferred = FulfillmentStatus.TRANSFERRED;
        Fulfillment fulfillment = controller.fulfillmentUpdated(ucsdPrincipal, 1L, transferred);
        Assert.assertEquals(transferred, fulfillment.getStatus());
    }

    @Test(expected = NotFoundException.class)
    @WithMockUser(UMIACS)
    public void fulfillmentUpdateNotFound() {
        controller.fulfillmentUpdated(umiacsPrincipal, 12L, FulfillmentStatus.FAILED);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void fulfillmentUpdateUnauthorizedXfer() {
        controller.fulfillmentUpdated(ncarPrincipal, 1L, FulfillmentStatus.TRANSFERRED);
    }

    @Test(expected = UnauthorizedException.class)
    @WithMockUser(NCAR)
    public void fulfillmentUpdateUnauthorized() {
        controller.fulfillmentUpdated(ncarPrincipal, 1L, FulfillmentStatus.FAILED);
    }

}