package org.chronopolis.ingest.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.api.serializer.BagSerializer;
import org.chronopolis.ingest.api.serializer.RepairSerializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeDeserializer;
import org.chronopolis.ingest.api.serializer.ZonedDateTimeSerializer;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.ingest.support.PageImpl;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Repair;
import org.chronopolis.rest.entities.fulfillment.Ace;
import org.chronopolis.rest.models.repair.ACEStrategy;
import org.chronopolis.rest.models.repair.AuditStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.security.Principal;
import java.time.ZonedDateTime;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for our repair controller
 *
 * Created by shake on 1/26/17.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(RepairController.class)
@ContextConfiguration(classes = WebContext.class)
public class RepairControllerTest extends IngestTest {
    private final Logger log = LoggerFactory.getLogger(RepairControllerTest.class);

    private static final String UNAUTHORIZED = "unauthorized";
    private static final String AUTHORIZED = "authorized";
    private static final String REQUESTER = "requester";

    private static Principal unauthorizedPrincipal = () -> UNAUTHORIZED;
    private static Principal authorizedPrincipal = () -> AUTHORIZED;
    private static Principal requesterPrincipal = () -> REQUESTER;

    private static UserDetails user = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_USER"));
    private static UserDetails admin = new User(AUTHORIZED, AUTHORIZED, ImmutableList.of(() -> "ROLE_ADMIN"));

    private MockMvc mvc;
    private RepairController controller;

    // Beans for the RepairController
    @MockBean private NodeRepository nodes;
    @MockBean private SearchService<Bag, Long, BagRepository> bags;
    @MockBean private SearchService<Repair, Long, RepairRepository> repairs;

    // Beans for our usage
    @MockBean private SecurityContext context;
    @MockBean private Authentication authentication;

    @Before
    public void setup() {
        SecurityContextHolder.setContext(context);

        // move to helper?
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.serializerByType(Bag.class, new BagSerializer());
        builder.serializerByType(Repair.class, new RepairSerializer());
        builder.serializerByType(ZonedDateTime.class, new ZonedDateTimeSerializer());
        builder.deserializerByType(ZonedDateTime.class, new ZonedDateTimeDeserializer());
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(builder.build());
        converter.setSupportedMediaTypes(ImmutableList.of(MediaType.APPLICATION_JSON));

        controller = new RepairController(bags, nodes, repairs);
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .build();
    }

    @Test
    public void getRepair() throws Exception {
        Repair unfulfilled = baseRepair();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(unfulfilled);
        mvc.perform(
                get("/api/repairs/{id}", unfulfilled.getId())
                        .principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getRepairNotFound() throws Exception {
        when(repairs.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(
                get("/api/repairs/{id}", 100L)
                    .principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is(404));
    }

    @Test
    public void getRepairs() throws Exception {
        when(repairs.findAll(any(SearchCriteria.class), any(Pageable.class))).thenReturn(new PageImpl<>());
        mvc.perform(get("/api/repairs").principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void createRepair() throws Exception {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        // requester instead of authorized?
        when(nodes.findByUsername(AUTHORIZED)).thenReturn(new Node(AUTHORIZED, AUTHORIZED));
        when(bags.find(any(SearchCriteria.class))).thenReturn(new Bag("test-bag", "test-depositor"));

        String json = "{\"depositor\":\"test-depositor\",\"collection\":\"bag-0\",\"files\":[\"test-file-1\"]}";
        mvc.perform(
                post("/api/repairs")
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.REQUESTED.name()))
                .andExpect(jsonPath("$.requester").value(AUTHORIZED))
                .andExpect(jsonPath("$.to").value(AUTHORIZED));
    }

    private <T> String asJson(T request) {
        ObjectMapper mapper = new Jackson2ObjectMapperBuilder()
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .build();
        try {
            return mapper.writeValueAsString(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void createRepairAdmin() throws Exception {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(admin);
        when(nodes.findByUsername(REQUESTER)).thenReturn(new Node(REQUESTER, REQUESTER));
        when(bags.find(any(SearchCriteria.class))).thenReturn(new Bag("test-bag", "test-depositor"));

        String json = "{\"to\":\"requester\",\"depositor\":\"test-depositor\",\"collection\":\"bag-0\",\"files\":[\"test-file-1\"]}";

        // Need to inject Mock SecurityContext here
        mvc.perform(
                post("/api/repairs")
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.requester").value(AUTHORIZED))
                .andExpect(jsonPath("$.to").value(REQUESTER));
    }

    @Test
    public void createRepairUnauthorized() throws Exception {
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(bags.find(any(SearchCriteria.class))).thenReturn(new Bag("test-bag", "test-depositor"));

        // For some reason the ObjectMapper isn't creating proper json for the RepairRequest class, so we'll just hard code it for now
        String json = "{\"to\":\"requester\",\"depositor\":\"test-depositor\",\"collection\":\"bag-0\",\"files\":[\"test-file-1\"]}";

        mvc.perform(post("/api/repairs").principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(json))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillRequest() throws Exception {
        Repair unfulfilled = baseRepair();
        when(nodes.findByUsername(AUTHORIZED)).thenReturn(new Node(AUTHORIZED, AUTHORIZED));
        when(repairs.find(any(SearchCriteria.class))).thenReturn(unfulfilled);
        mvc.perform(post("/api/repairs/{id}/fulfill", unfulfilled.getId()).principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.from").value(AUTHORIZED));

        // verify...
    }

    @Test
    public void fulfillOwnRequest() throws Exception {
        Repair unfulfilled = baseRepair();
        when(nodes.findByUsername(REQUESTER)).thenReturn(new Node(REQUESTER, REQUESTER));
        when(repairs.find(any(SearchCriteria.class))).thenReturn(unfulfilled);

        mvc.perform(post("/api/repairs/{id}/fulfill", unfulfilled.getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    public void fulfillRequestConflict() throws Exception {
        Repair fulfilling = fulfilling();
        when(nodes.findByUsername(UNAUTHORIZED)).thenReturn(new Node(UNAUTHORIZED, UNAUTHORIZED));
        when(repairs.find(any(SearchCriteria.class))).thenReturn(fulfilling);

        mvc.perform(post("/api/repairs/{id}/fulfill", fulfilling.getId()).principal(unauthorizedPrincipal))
                .andDo(print())
                .andExpect(status().isConflict());
    }


    @Test
    public void readyFulfillment() throws Exception {
        Repair fulfilling = fulfilling();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(fulfilling);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        mvc.perform(
                put("/api/repairs/{id}/ready", fulfilling.getId())
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(strategy)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.type").value(FulfillmentType.ACE.name()));
    }

    @Test
    public void readyFulfillmentUnauthorized() throws Exception {
        Repair fulfilling = fulfilling();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(fulfilling);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        ACEStrategy strategy = new ACEStrategy()
                .setApiKey("test-api-key")
                .setUrl("test-url");

        mvc.perform(
                put("/api/repairs/{id}/ready", fulfilling.getId())
                        .principal(unauthorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(strategy)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void completeFulfillmentNoStrategy() throws Exception {
        Repair fulfilling = fulfilling();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(fulfilling);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        mvc.perform(put("/api/repairs/{id}/complete", fulfilling.getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }


    @Test
    public void completeFulfillment() throws Exception {
        Repair completing = completing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(completing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(put("/api/repairs/{id}/complete", completing.getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.REPAIRED.name()));
    }

    @Test
    public void completeFulfillmentUnauthorized() throws Exception {
        Repair completing = completing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(completing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(put("/api/repairs/{id}/complete", completing.getId()).principal(unauthorizedPrincipal))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairAuditing() throws Exception {
        Repair auditing = auditing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(auditing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(
                put("/api/repairs/{id}/audit", auditing.getId())
                        .principal(requesterPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(AuditStatus.AUDITING)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.audit").value(AuditStatus.AUDITING.name()));
    }

    @Test
    public void repairAuditingNotFound() throws Exception {
        Repair auditing = auditing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(
                put("/api/repairs/{id}/audit", auditing.getId())
                        .principal(requesterPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(AuditStatus.AUDITING)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairAuditingUnauthorized() throws Exception {
        Repair auditing = auditing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(auditing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(
                put("/api/repairs/{id}/audit",auditing.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(AuditStatus.AUDITING)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairCleaned() throws Exception {
        Repair cleaning = cleaning();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(cleaning);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(
                put("/api/repairs/{id}/cleaned", cleaning.getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.cleaned").value(true));
    }

    @Test
    public void repairCleanNotFound() throws Exception {
        when(repairs.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/cleaned", cleaning().getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairCleanUnauthorized() throws Exception {
        Repair cleaning = cleaning();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(cleaning());
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(put("/api/repairs/{id}/cleaned", cleaning.getId()).principal(unauthorizedPrincipal))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void repairReplaced() throws Exception {
        Repair replacing = replacing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(replacing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(put("/api/repairs/{id}/replaced", replacing.getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.replaced").value(true));
    }

    @Test
    public void repairReplacedNotFound() throws Exception {
        when(repairs.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/replaced", replacing().getId()).principal(requesterPrincipal))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void repairReplacedUnauthorized() throws Exception {
        Repair replacing = replacing();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(replacing);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);

        mvc.perform(put("/api/repairs/{id}/replaced", replacing.getId()).principal(unauthorizedPrincipal))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillmentUpdated() throws Exception {
        Repair fulfilling = fulfilling();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(fulfilling);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        mvc.perform(
                put("/api/repairs/{id}/status", fulfilling.getId())
                    .principal(authorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.READY)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.READY.name()));
    }

    @Test
    public void fulfillmentUpdatedXfer() throws Exception {
        Repair transferred = transferred();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(transferred);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        mvc.perform(put("/api/repairs/{id}/status", transferred.getId())
                .principal(requesterPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                .andDo(print())
                .andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.status").value(RepairStatus.TRANSFERRED.name()));
    }

    @Test
    public void fulfillmentUpdateNotFound() throws Exception {
        when(repairs.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(put("/api/repairs/{id}/status", baseRepair().getId())
                .principal(authorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    public void fulfillmentUpdateUnauthorizedXfer() throws Exception {
        Repair transferred = transferred();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(transferred);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        mvc.perform(
                put("/api/repairs/{id}/status", transferred.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.TRANSFERRED)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void fulfillmentUpdateUnauthorized() throws Exception {
        Repair transferred = transferred();
        when(repairs.find(any(SearchCriteria.class))).thenReturn(transferred);
        when(context.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        mvc.perform(
                put("/api/repairs/{id}/status", transferred.getId())
                        .principal(unauthorizedPrincipal).contentType(MediaType.APPLICATION_JSON).content(asJson(RepairStatus.FAILED)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    private Repair baseRepair() {
        Repair repair = new Repair();
        repair.setBag(new Bag("test-bag", "test-depositor"));
        repair.setId(1L);
        repair.setRequester(REQUESTER);
        repair.setFiles(ImmutableSet.of());
        repair.setStatus(RepairStatus.READY);
        repair.setTo(new Node(REQUESTER, REQUESTER));
        return repair;
    }

    private Repair fulfilling() {
        return baseRepair()
                .setStatus(RepairStatus.STAGING)
                .setFrom(new Node(AUTHORIZED, AUTHORIZED));
    }

    private Repair transferred() {
        return baseRepair()
                .setStatus(RepairStatus.TRANSFERRED)
                .setFrom(new Node(AUTHORIZED, AUTHORIZED))
                .setStrategy(new Ace());
    }

    private Repair auditing () {
        return transferred()
                .setValidated(true);
    }

    private Repair replacing() {
        return auditing()
                .setAudit(AuditStatus.SUCCESS);
    }

    private Repair completing() {
        return replacing()
                .setReplaced(true);
    }

    private Repair cleaning() {
        return completing()
                .setStatus(RepairStatus.REPAIRED);
    }

}