package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.DepositorContact;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.QDepositorContact;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.models.DepositorContactCreate;
import org.chronopolis.rest.models.DepositorContactRemove;
import org.chronopolis.rest.models.DepositorCreate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;

import java.security.Principal;
import java.time.ZonedDateTime;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the DepositorController
 * <p>
 */
@RunWith(SpringRunner.class)
@WebMvcTest(DepositorController.class)
public class DepositorControllerTest extends ControllerTest {
    // Immutable fields used for testing
    private static final String EMAIL = "fake-account@umiacs.umd.edu";
    private static final String ADDRESS = "test-address";
    private static final String BAG_NAME = "test-bag";
    private static final String NODE_NAME = "node-name";
    private static final String NAMESPACE = "test-depositor";
    private static final String ORGANIZATION = "test-organization";
    private static final String DEPOSITOR_ROOT_PATH = "/api/depositors";
    private static final String DEPOSITOR_BAGS_PATH = "/api/depositors/{namespace}/bags";
    private static final String DEPOSITOR_BAG_NAME_PATH = "/api/depositors/{namespace}/bags/{bagName}";
    private static final String DEPOSITOR_NAMESPACE_PATH = "/api/depositors/{namespace}";
    private static final String NODE_PATH = "/api/depositors/{namespace}/nodes/{nodeName}";
    private static final String CONTACT_PATH = "/api/depositors/{namespace}/contacts";
    private static final ZonedDateTime NOW = ZonedDateTime.now();

    private static final QBag Q_BAG = QBag.bag;
    private static final QNode Q_NODE = QNode.node;
    private static final QDepositor Q_DEPOSITOR = QDepositor.depositor;
    private static final QDepositorContact Q_CONTACT = QDepositorContact.depositorContact;


    // Fields we set up
    private Bag bag;
    private Node node;
    private Depositor depositor = new Depositor();
    private DepositorContact contact = new DepositorContact();
    private BooleanExpression namespaceEq = Q_DEPOSITOR.namespace.eq(NAMESPACE);
    private BooleanExpression contactEq = Q_CONTACT.depositor.namespace.eq(NAMESPACE)
            .and(Q_CONTACT.contactEmail.eq(EMAIL));
    private BooleanExpression nodeEq = Q_NODE.username.eq(NODE_NAME);

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

        node = new Node(NODE_NAME, NODE_NAME);

        bag = new Bag(BAG_NAME, depositor)
                .setCreator(NAMESPACE)
                .setSize(1L)
                .setTotalFiles(1L);
        bag.setCreatedAt(NOW)
                .setUpdatedAt(NOW)
                .setId(1L);

        DepositorController controller = new DepositorController(dao);
        setupMvc(controller);
    }

    @Test
    public void testCreate() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isCreated());
        verify(dao, times(1)).save(eq(depositor));
    }

    @Test
    public void testCreateInvalidPhoneNumber() throws Exception {
        DepositorCreate model = createModel(false);
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isBadRequest());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testCreateForbidden() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateUser();
        runPost(DEPOSITOR_ROOT_PATH, unauthorizedPrincipal, model)
                .andExpect(status().isForbidden());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testCreateConflict() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isConflict());
        verify(dao, times(0)).save(eq(depositor));
    }

    @Test
    public void testGet() throws Exception {
        when(dao.findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(depositor)));
        mvc.perform(get(DEPOSITOR_ROOT_PATH)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetByNamespace() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(depositor);
        mvc.perform(get(DEPOSITOR_NAMESPACE_PATH, NAMESPACE)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk())
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
                .principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetBags() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(Predicate.class)))
                .thenReturn(depositor);
        when(dao.findPage(eq(Q_BAG), any(BagFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(bag)));
        mvc.perform(get(DEPOSITOR_BAGS_PATH, NAMESPACE)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findPage(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testGetBagsNotFound() throws Exception {
        mvc.perform(get(DEPOSITOR_BAGS_PATH, NAMESPACE)
                .principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(0)).findPage(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testGetBag() throws Exception {
        when(dao.findOne(eq(Q_BAG), any(BagFilter.class)))
                .thenReturn(bag);
        mvc.perform(get(DEPOSITOR_BAG_NAME_PATH, NAMESPACE, BAG_NAME)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk())
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
                .principal(authorizedPrincipal))
                .andExpect(status().isNotFound());
        verify(dao, times(1)).findOne(eq(Q_BAG), any(BagFilter.class));
    }

    @Test
    public void testAddContact() throws Exception {
        DepositorContactCreate contactCreate = contactModel(true);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);

        authenticateAdmin();
        runPost(CONTACT_PATH, authorizedPrincipal, contactCreate, NAMESPACE)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contactName").value("test-name"));

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
        verify(dao, times(1)).save(any(Depositor.class));
    }

    @Test
    public void testAddContactBadRequest() throws Exception {
        DepositorContactCreate contactCreate = contactModel(false);

        authenticateAdmin();
        runPost(CONTACT_PATH, authorizedPrincipal, contactCreate, NAMESPACE)
                .andExpect(status().isBadRequest());
        verify(dao, times(0)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(0)).findOne(eq(Q_CONTACT), eq(contactEq));
        verify(dao, times(0)).save(any(Depositor.class));
    }

    @Test
    public void testAddContactForbidden() throws Exception {
        DepositorContactCreate contactCreate = contactModel(true);

        authenticateUser();
        runPost(CONTACT_PATH, authorizedPrincipal, contactCreate, NAMESPACE)
                .andExpect(status().isForbidden());
        verify(dao, times(0)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(0)).save(any(Depositor.class));
    }

    @Test
    public void testAddContactNotFound() throws Exception {
        DepositorContactCreate contactCreate = contactModel(true);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(null);

        authenticateAdmin();
        runPost(CONTACT_PATH, authorizedPrincipal, contactCreate, NAMESPACE)
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
        verify(dao, times(0)).save(any(Depositor.class));
    }

    @Test
    public void testAddContactConflict() throws Exception {
        DepositorContactCreate contactCreate = contactModel(true);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(null);
        when(dao.findOne(eq(Q_CONTACT), eq(contactEq)))
                .thenReturn(contact);

        authenticateAdmin();
        runPost(CONTACT_PATH, authorizedPrincipal, contactCreate, NAMESPACE)
                .andExpect(status().isConflict());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
        verify(dao, times(0)).save(any(Depositor.class));
    }


    @Test
    public void testRemoveContact() throws Exception {
        DepositorContactRemove remove = new DepositorContactRemove(EMAIL);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        when(dao.findOne(eq(Q_CONTACT), eq(contactEq))).thenReturn(contact);

        authenticateAdmin();
        mvc.perform(
                delete(CONTACT_PATH, NAMESPACE)
                        .principal(authorizedPrincipal)
                        .contentType(APPLICATION_JSON)
                        .content(asJson(remove)))
        .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
    }

    public void testRemoveContactNotFound() throws Exception {
        DepositorContactRemove remove = new DepositorContactRemove(EMAIL);

        authenticateAdmin();
        mvc.perform(
                delete(CONTACT_PATH, NAMESPACE)
                        .principal(authorizedPrincipal)
                        .contentType(APPLICATION_JSON)
                        .content(asJson(remove)))
        .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
    }

    public void testRemoveContactBadRequest() throws Exception {
        DepositorContactRemove remove = new DepositorContactRemove(EMAIL);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);

        authenticateAdmin();
        mvc.perform(
                delete(CONTACT_PATH, NAMESPACE)
                        .principal(authorizedPrincipal)
                        .contentType(APPLICATION_JSON)
                        .content(asJson(remove)))
        .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_CONTACT), eq(contactEq));
    }

    @Test
    public void testRemoveContactForbidden() throws Exception {
        DepositorContactRemove remove = new DepositorContactRemove(EMAIL);

        authenticateUser();
        mvc.perform(
                delete(CONTACT_PATH, NAMESPACE)
                        .principal(authorizedPrincipal)
                        .contentType(APPLICATION_JSON)
                        .content(asJson(remove)))
        .andExpect(status().isForbidden());
    }

    @Test
    public void testAddNode() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        when(dao.findOne(eq(Q_NODE), eq(nodeEq)))
                .thenReturn(node);
        authenticateAdmin();
        mvc.perform(post(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testAddNodeNotFound() throws Exception {
        authenticateAdmin();
        mvc.perform(post(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testAddNodeBadRequest() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        authenticateAdmin();
        mvc.perform(post(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testAddNodeForbidden() throws Exception {
        authenticateUser();
        mvc.perform(post(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isForbidden());

        verify(dao, times(0)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(0)).findOne(eq(Q_NODE), eq(nodeEq));
    }


    @Test
    public void testRemoveNode() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        when(dao.findOne(eq(Q_NODE), eq(nodeEq)))
                .thenReturn(node);
        authenticateAdmin();
        mvc.perform(delete(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testRemoveNodeNotFound() throws Exception {
        authenticateAdmin();
        mvc.perform(delete(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isNotFound());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testRemoveNodeBadRequest() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(depositor);
        authenticateAdmin();
        mvc.perform(delete(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isBadRequest());

        verify(dao, times(1)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(1)).findOne(eq(Q_NODE), eq(nodeEq));
    }

    @Test
    public void testRemoveNodeForbidden() throws Exception {
        authenticateUser();
        mvc.perform(delete(NODE_PATH, NAMESPACE, NODE_NAME).principal(authorizedPrincipal))
                .andExpect(status().isForbidden());

        verify(dao, times(0)).findOne(eq(Q_DEPOSITOR), eq(namespaceEq));
        verify(dao, times(0)).findOne(eq(Q_NODE), eq(nodeEq));
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
                .setEmail(EMAIL)
                .setName("test-name")
                .setPhoneNumber(new DepositorContactCreate.PhoneNumber()
                        // from libphonenumber doc - swiss google number
                        .setCountryCode("CH")
                        .setNumber("446681800"));
        } else {
            return new DepositorContactCreate()
                .setEmail(EMAIL)
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
                                      T content,
                                      Object... vars) throws Exception {
        return mvc.perform(post(path, vars)
                .principal(principal)
                .contentType(APPLICATION_JSON)
                .content(asJson(content)));
    }

}