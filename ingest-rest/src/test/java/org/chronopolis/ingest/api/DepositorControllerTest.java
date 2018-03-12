package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorCreate;
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
import org.springframework.test.web.servlet.ResultActions;

import java.security.Principal;
import java.time.ZonedDateTime;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the DepositorController
 * <p>
 * todo: create - 403
 * todo: create - 409
 */
@RunWith(SpringRunner.class)
@WebMvcTest(DepositorController.class)
public class DepositorControllerTest extends ControllerTest {
    private final Logger log = LoggerFactory.getLogger(DepositorControllerTest.class);

    // Immutable fields used for testing
    private static final String ADDRESS = "test-address";
    private static final String BAG_NAME = "test-bag";
    private static final String NAMESPACE = "test-depositor";
    private static final String ORGANIZATION = "test-organization";
    private static final String DEPOSITOR_ROOT_PATH = "/api/depositors";
    private static final String DEPOSITOR_BAGS_PATH = "/api/depositors/{namespace}/bags";
    private static final String DEPOSITOR_BAG_NAME_PATH = "/api/depositors/{namespace}/bags/{bagName}";
    private static final String DEPOSITOR_NAMESPACE_PATH = "/api/depositors/{namespace}";
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    private static final QBag Q_BAG = QBag.bag;
    private static final QDepositor Q_DEPOSITOR = QDepositor.depositor;

    // Field we set up
    private Bag bag;
    private DepositorController controller;
    private Depositor depositor = new Depositor();

    // Mocks for the Controller
    @MockBean private PagedDAO dao;

    @Before
    public void setup() {
        depositor.setNamespace(NAMESPACE)
                .setOrganizationAddress(ADDRESS)
                .setSourceOrganization(ORGANIZATION)
                .setCreatedAt(NOW)
                .setUpdatedAt(NOW)
                .setId(1L);

        bag = new Bag(BAG_NAME, depositor)
                .setCreator(NAMESPACE)
                .setSize(1L)
                .setTotalFiles(1L);
        bag.setCreatedAt(NOW)
                .setUpdatedAt(NOW)
                .setId(1L);

        controller = new DepositorController(dao);
        setupMvc(controller);
    }

    @Test
    public void testCreate() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        runPost(DEPOSITOR_ROOT_PATH, () -> "user", model)
                .andExpect(status().isCreated());
        verify(dao, times(1)).save(eq(depositor));
    }

    @Test
    public void testCreateInvalidPhoneNumber() throws Exception {
        DepositorCreate model = createModel(false);
        runPost(DEPOSITOR_ROOT_PATH, () -> "user", model)
                .andExpect(status().isBadRequest());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testCreateForbidden() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateUser();
        runPost(DEPOSITOR_ROOT_PATH, () -> "user", model)
                .andExpect(status().isForbidden());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testCreateConflict() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        when(dao.findOne(eq(Q_DEPOSITOR), eq(Q_DEPOSITOR.namespace.eq(NAMESPACE))))
                .thenReturn(depositor);
        runPost(DEPOSITOR_ROOT_PATH, () -> "user", model)
                .andExpect(status().isConflict());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testGet() throws Exception {
        when(dao.findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(depositor)));
        mvc.perform(get(DEPOSITOR_ROOT_PATH)
                .principal(() -> "user"))
                .andExpect(status().is(200));

        verify(dao, times(1)).findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetByNamespace() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(depositor);
        mvc.perform(get(DEPOSITOR_NAMESPACE_PATH, NAMESPACE)
                .principal(() -> "user"))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.namespace").value(NAMESPACE))
                .andExpect(jsonPath("$.sourceOrganization").value(ORGANIZATION))
                .andExpect(jsonPath("$.organizationAddress").value(ADDRESS));

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetByNamespaceNotFound() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(null);
        mvc.perform(get(DEPOSITOR_NAMESPACE_PATH, NAMESPACE)
                .principal(() -> "user"))
                .andExpect(status().is(404));

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetBags() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(Predicate.class)))
                .thenReturn(depositor);
        when(dao.findPage(eq(Q_BAG), any(BagFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(bag)));
        mvc.perform(get(DEPOSITOR_BAGS_PATH, NAMESPACE)
                .principal(() -> "user"))
                .andExpect(status().is(200));

        verify(dao, times(1)).findPage(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testGetBagsNotFound() throws Exception {
        mvc.perform(get(DEPOSITOR_BAGS_PATH, NAMESPACE)
                .principal(() -> "user"))
                .andExpect(status().isNotFound());

        verify(dao, times(0)).findPage(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testGetBag() throws Exception {
        when(dao.findOne(eq(Q_BAG), any(BagFilter.class)))
                .thenReturn(bag);
        mvc.perform(get(DEPOSITOR_BAG_NAME_PATH, NAMESPACE, BAG_NAME)
                .principal(() -> "user"))
                .andExpect(status().is(200))
                // don't need to check all because that's done in the BagControllerTest
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value(BAG_NAME))
                .andExpect(jsonPath("$.depositor").value(NAMESPACE))
                .andExpect(jsonPath("$.creator").value(NAMESPACE));

        verify(dao, times(1)).findOne(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testGetBagNotFound() throws Exception {
         when(dao.findOne(eq(Q_BAG), any(BagFilter.class)))
                .thenReturn(null);
        mvc.perform(get(DEPOSITOR_BAG_NAME_PATH, NAMESPACE, BAG_NAME)
                .principal(() -> "user"))
                .andExpect(status().is(404));
        verify(dao, times(1)).findOne(eq(Q_BAG), any(BagFilter.class));
    }


    // Helpers
    private DepositorCreate createModel(boolean valid) {
        return new DepositorCreate()
                .setNamespace(NAMESPACE)
                .setSourceOrganization(ORGANIZATION)
                .setOrganizationAddress(ADDRESS)
                .setContacts(ImmutableList.of(contactModel(valid)));
    }

    private DepositorContactCreate contactModel(boolean valid) {
        if (valid) {
           return new DepositorContactCreate()
                .setEmail("fake-account@umiacs.umd.edu")
                .setName("test-name")
                .setPhoneNumber(new DepositorContactCreate.PhoneNumber()
                        // from libphonenumber doc - swiss google number
                        .setCountryCode("CH")
                        .setNumber("446681800"));
        } else {
            return new DepositorContactCreate()
                .setEmail("fake-account@umiacs.umd.edu")
                .setName("test-name")
                .setPhoneNumber(new DepositorContactCreate.PhoneNumber()
                        .setCountryCode("US")
                        .setNumber("0"));
        }
    }

    // could possible push this into ControllerTest, not strictly needed though and may simply
    // obfuscate the boilerplate we use for testing
    private <T> ResultActions runPost(String path,
                                      Principal principal,
                                      T content) throws Exception {
        return mvc.perform(post(path)
                .principal(principal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(content)));
    }

}