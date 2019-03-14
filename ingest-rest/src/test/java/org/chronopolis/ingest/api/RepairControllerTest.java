package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.models.filter.RepairFilter;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.repair.Ace;
import org.chronopolis.rest.entities.repair.QRepair;
import org.chronopolis.rest.entities.repair.Repair;
import org.chronopolis.rest.models.AceStrategy;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.FulfillmentType;
import org.chronopolis.rest.models.enums.RepairStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for our repair controller
 *
 * Created by shake on 1/26/17.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(RepairController.class)
public class RepairControllerTest extends ControllerTest {

    private final Logger log = LoggerFactory.getLogger(RepairControllerTest.class);

    private final Depositor depositor = new Depositor();

    // Beans for the RepairController
    @MockBean private PagedDao dao;

    @Before
    public void setup() {
        RepairController controller = new RepairController(dao);
        setupMvc(controller);
    }

    @Test
    public void getRepair() throws Exception {
        Repair unfulfilled = baseRepair();
        when(dao.findOne(any(), any(Predicate.class))).thenReturn(unfulfilled);
        mvc.perform(
                get("/api/repairs/{id}", unfulfilled.getId())
                        .principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getRepairNotFound() throws Exception {
        when(dao.findOne(any(), any(Predicate.class))).thenReturn(null);
        mvc.perform(
                get("/api/repairs/{id}", 100L)
                    .principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(404));
    }

    @Test
    public void getRepairs() throws Exception {
        when(dao.findPage(any(), any(RepairFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of()));
        mvc.perform(get("/api/repairs").principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void createRepair() throws Exception {
        authenticateUser();
        // requester instead of authorized?
        Node node = new Node(of(), AUTHORIZED, AUTHORIZED, true);
        Bag bag = new Bag("test-bag", "test-creator", depositor, 0L, 0L, BagStatus.DEPOSITED);
        when(dao.findOne(eq(QNode.node), any(Predicate.class))).thenReturn(node);
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(bag);

        String json = "{\"to\":\"authorized\"," +
                "\"depositor\":\"test-depositor\"," +
                "\"collection\":\"bag-0\"," +
                "\"files\":[\"test-file-1\"]}";
        mvc.perform(
                post("/api/repairs")
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.REQUESTED.name()))
                .andExpect(jsonPath("$.requester").value(AUTHORIZED))
                .andExpect(jsonPath("$.to").value(AUTHORIZED));
    }

    @Test
    public void createRepairAdmin() throws Exception {
        authenticateAdmin();
        Node node = new Node(of(), REQUESTER, REQUESTER, true);
        Bag bag = new Bag("test-bag", "test-creator", depositor, 0L, 0L, BagStatus.DEPOSITED);
        when(dao.findOne(eq(QNode.node), any(Predicate.class))).thenReturn(node);
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(bag);

        String json = "{\"to\":\"requester\",\"depositor\":\"test-depositor\",\"collection\":\"bag-0\",\"files\":[\"test-file-1\"]}";

        // Need to inject Mock SecurityContext here
        mvc.perform(
                post("/api/repairs")
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.requester").value(AUTHORIZED))
                .andExpect(jsonPath("$.to").value(REQUESTER));
    }

    @Test
    public void createRepairUnauthorized() throws Exception {
        authenticateUser();
        Bag bag = new Bag("test-bag", "test-creator", depositor, 0L, 0L, BagStatus.DEPOSITED);
        when(dao.findOne(eq(QBag.bag), any(Predicate.class))).thenReturn(bag);

        // For some reason the ObjectMapper isn't creating proper json for the RepairRequest class, so we'll just hard code it for now
        String json = "{\"to\":\"requester\",\"depositor\":\"test-depositor\",\"collection\":\"bag-0\",\"files\":[\"test-file-1\"]}";

        mvc.perform(post("/api/repairs").principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(json))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillRequest() throws Exception {
        Repair unfulfilled = baseRepair();
        Node node = new Node(of(), AUTHORIZED, AUTHORIZED, true);
        when(dao.findOne(eq(QNode.node), any(Predicate.class))).thenReturn(node);
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(unfulfilled);
        mvc.perform(post("/api/repairs/{id}/fulfill", unfulfilled.getId()).principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.from").value(AUTHORIZED));

        // verify...
    }

    @Test
    public void fulfillOwnRequest() throws Exception {
        Repair unfulfilled = baseRepair();
        Node node = new Node(of(), REQUESTER, REQUESTER, true);
        when(dao.findOne(eq(QNode.node), any(Predicate.class))).thenReturn(node);
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(unfulfilled);

        mvc.perform(post("/api/repairs/{id}/fulfill", unfulfilled.getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fulfillRequestConflict() throws Exception {
        Repair fulfilling = fulfilling();
        Node node = new Node(of(), AUTHORIZED, UNAUTHORIZED, true);
        when(dao.findOne(eq(QNode.node), any(Predicate.class))).thenReturn(node);
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(fulfilling);

        mvc.perform(post("/api/repairs/{id}/fulfill", fulfilling.getId()).principal(unauthorizedPrincipal))
                // .andDo(print())
                .andExpect(status().isConflict());
    }


    @Test
    public void readyFulfillment() throws Exception {
        Repair fulfilling = fulfilling();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(fulfilling);
        authenticateUser();

        AceStrategy strategy = new AceStrategy("test-api-key", "test-url");

        log.info("{}", asJson(strategy));

        mvc.perform(
                put("/api/repairs/{id}/ready", fulfilling.getId())
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(strategy)))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.type").value(FulfillmentType.ACE.name()));
    }

    @Test
    public void readyFulfillmentUnauthorized() throws Exception {
        Repair fulfilling = fulfilling();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(fulfilling);
        authenticateUser();

        AceStrategy strategy = new AceStrategy("test-api-key", "test-url");

        log.info("{}", asJson(strategy));

        mvc.perform(
                put("/api/repairs/{id}/ready", fulfilling.getId())
                        .principal(unauthorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(strategy)))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void completeFulfillmentNoStrategy() throws Exception {
        Repair fulfilling = fulfilling();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(fulfilling);
        authenticateUser();
        mvc.perform(put("/api/repairs/{id}/complete", fulfilling.getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().isBadRequest());
    }


    @Test
    public void completeFulfillment() throws Exception {
        Repair completing = completing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(completing);
        authenticateUser();

        mvc.perform(put("/api/repairs/{id}/complete", completing.getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.REPAIRED.name()));
    }

    @Test
    public void completeFulfillmentUnauthorized() throws Exception {
        Repair completing = completing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(completing);
        authenticateUser();

        mvc.perform(put("/api/repairs/{id}/complete", completing.getId()).principal(unauthorizedPrincipal))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairAuditing() throws Exception {
        Repair auditing = auditing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(auditing);
        authenticateUser();

        mvc.perform(
                put("/api/repairs/{id}/audit", auditing.getId())
                        .principal(requesterPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(AuditStatus.AUDITING)))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.audit").value(AuditStatus.AUDITING.name()));
    }

    @Test
    public void repairAuditingNotFound() throws Exception {
        Repair auditing = auditing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(null);
        mvc.perform(
                put("/api/repairs/{id}/audit", auditing.getId())
                        .principal(requesterPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(AuditStatus.AUDITING)))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairAuditingUnauthorized() throws Exception {
        Repair auditing = auditing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(auditing);
        authenticateUser();

        mvc.perform(
                put("/api/repairs/{id}/audit",auditing.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(AuditStatus.AUDITING)))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairCleaned() throws Exception {
        Repair cleaning = cleaning();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(cleaning);
        authenticateUser();

        mvc.perform(
                put("/api/repairs/{id}/cleaned", cleaning.getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.cleaned").value(true));
    }

    @Test
    public void repairCleanNotFound() throws Exception {
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/cleaned", cleaning().getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairCleanUnauthorized() throws Exception {
        Repair cleaning = cleaning();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(cleaning);
        authenticateUser();

        mvc.perform(put("/api/repairs/{id}/cleaned", cleaning.getId()).principal(unauthorizedPrincipal))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairReplaced() throws Exception {
        Repair replacing = replacing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(replacing);
        authenticateUser();

        mvc.perform(put("/api/repairs/{id}/replaced", replacing.getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.replaced").value(true));
    }

    @Test
    public void repairReplacedNotFound() throws Exception {
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/replaced", replacing().getId()).principal(requesterPrincipal))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairReplacedUnauthorized() throws Exception {
        Repair replacing = replacing();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(replacing);
        authenticateUser();

        mvc.perform(put("/api/repairs/{id}/replaced", replacing.getId()).principal(unauthorizedPrincipal))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillmentUpdated() throws Exception {
        Repair fulfilling = fulfilling();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(fulfilling);
        authenticateUser();
        mvc.perform(
                put("/api/repairs/{id}/status", fulfilling.getId())
                    .principal(authorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.READY)))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.READY.name()));
    }

    @Test
    public void fulfillmentUpdatedXfer() throws Exception {
        Repair transferred = transferred();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(transferred);
        authenticateUser();
        mvc.perform(put("/api/repairs/{id}/status", transferred.getId())
                .principal(requesterPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                // .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.TRANSFERRED.name()));
    }

    @Test
    public void fulfillmentUpdateNotFound() throws Exception {
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/status", baseRepair().getId())
                .principal(authorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                // .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void fulfillmentUpdateUnauthorizedXfer() throws Exception {
        Repair transferred = transferred();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(transferred);
        authenticateUser();
        mvc.perform(
                put("/api/repairs/{id}/status", transferred.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillmentUpdateUnauthorized() throws Exception {
        Repair transferred = transferred();
        when(dao.findOne(eq(QRepair.repair), any(Predicate.class))).thenReturn(transferred);
        authenticateUser();
        mvc.perform(
                put("/api/repairs/{id}/status", transferred.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.FAILED)))
                // .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    // maybe look at trimming some of the fat here
    private Repair baseRepair() {
        Node node = new Node(of(), REQUESTER, REQUESTER, true);
        Bag bag = new Bag("test-bag", "test-creator", depositor, 0L, 0L, BagStatus.DEPOSITED);
        Repair repair = new Repair();
        repair.setBag(bag);
        repair.setId(1L);
        repair.setRequester(REQUESTER);
        repair.setFiles(of());
        repair.setStatus(RepairStatus.READY);
        repair.setTo(node);
        return repair;
    }

    private Repair fulfilling() {
        Node node = new Node(of(), AUTHORIZED, AUTHORIZED, true);
        Repair repair = baseRepair();
        repair.setStatus(RepairStatus.STAGING);
        repair.setFrom(node);
        return repair;
    }

    private Repair transferred() {
        Node node = new Node(of(), REQUESTER, REQUESTER, true);
        Repair repair = baseRepair();
        repair.setStatus(RepairStatus.TRANSFERRED);
        repair.setFrom(node);
        repair.setStrategy(new Ace());
        return repair;
    }

    private Repair auditing () {
        Repair repair = transferred();
        repair.setValidated(true);
        return repair;
    }

    private Repair replacing() {
        Repair repair = auditing();
        repair.setAudit(AuditStatus.SUCCESS);
        return repair;
    }

    private Repair completing() {
        Repair repair = replacing();
        repair.setReplaced(true);
        return repair;
    }

    private Repair cleaning() {
        Repair repair = completing();
        repair.setStatus(RepairStatus.REPAIRED);
        return repair;
    }

}