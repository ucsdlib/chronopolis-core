package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.models.filter.BagFilter;
import org.chronopolis.ingest.models.filter.DepositorFilter;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.DepositorContact;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.depositor.QDepositorContact;
import org.chronopolis.rest.models.Phone;
import org.chronopolis.rest.models.create.DepositorContactCreate;
import org.chronopolis.rest.models.create.DepositorCreate;
import org.chronopolis.rest.models.delete.DepositorContactDelete;
import org.chronopolis.rest.models.enums.BagStatus;
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

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    private static final String BAG_NAME = "test-bag";
    private static final String NODE_NAME = "node-name";
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
    private final DepositorContact contact = new DepositorContact();
    private final BooleanExpression namespaceEq = Q_DEPOSITOR.namespace.eq(NAMESPACE);
    private final BooleanExpression contactEq = Q_CONTACT.depositor.namespace.eq(NAMESPACE)
            .and(Q_CONTACT.contactEmail.eq(EMAIL));
    private final BooleanExpression nodeEq = Q_NODE.username.eq(NODE_NAME);

    // Mocks for the Controller
    @MockBean private PagedDao dao;

    @Before
    public void setup() {
        node = new Node(of(), NODE_NAME, NODE_NAME, true);

        bag = new Bag(BAG_NAME, NAMESPACE, DEPOSITOR, 1L, 1L, BagStatus.DEPOSITED);
        bag.setCreatedAt(NOW);
        bag.setUpdatedAt(NOW);
        bag.setId(1L);

        DepositorController controller = new DepositorController(dao);
        setupMvc(controller);
    }

    @Test
    public void testCreate() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isCreated());
        verify(dao, times(1)).save(eq(DEPOSITOR));
    }

    @Test
    public void testCreateInvalidPhoneNumber() throws Exception {
        DepositorCreate model = createModel(false);
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isBadRequest());
        verify(dao, times(0)).save(eq(DEPOSITOR));
    }

    @Test
    public void testCreateForbidden() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateUser();
        runPost(DEPOSITOR_ROOT_PATH, unauthorizedPrincipal, model)
                .andExpect(status().isForbidden());
        verify(dao, times(0)).save(eq(DEPOSITOR));
    }

    @Test
    public void testCreateConflict() throws Exception {
        DepositorCreate model = createModel(true);
        authenticateAdmin();
        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(DEPOSITOR);
        runPost(DEPOSITOR_ROOT_PATH, authorizedPrincipal, model)
                .andExpect(status().isConflict());
        verify(dao, times(0)).save(eq(DEPOSITOR));
    }

    @Test
    public void testGet() throws Exception {
        when(dao.findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(new PageImpl<>(ImmutableList.of(DEPOSITOR)));
        mvc.perform(get(DEPOSITOR_ROOT_PATH)
                .principal(authorizedPrincipal))
                .andExpect(status().isOk());

        verify(dao, times(1)).findPage(eq(Q_DEPOSITOR), any(DepositorFilter.class));
    }

    @Test
    public void testGetByNamespace() throws Exception {
        when(dao.findOne(eq(Q_DEPOSITOR), any(DepositorFilter.class)))
                .thenReturn(DEPOSITOR);
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
                .thenReturn(DEPOSITOR);
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
                .thenReturn(DEPOSITOR);

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
        DepositorContactDelete remove = new DepositorContactDelete(EMAIL);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(DEPOSITOR);
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

    @Test
    public void testRemoveContactNotFound() throws Exception {
        DepositorContactDelete remove = new DepositorContactDelete(EMAIL);

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

    @Test
    public void testRemoveContactBadRequest() throws Exception {
        DepositorContactDelete remove = new DepositorContactDelete(EMAIL);

        when(dao.findOne(eq(Q_DEPOSITOR), eq(namespaceEq)))
                .thenReturn(DEPOSITOR);

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
        DepositorContactDelete remove = new DepositorContactDelete(EMAIL);

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
                .thenReturn(DEPOSITOR);
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
                .thenReturn(DEPOSITOR);
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
                .thenReturn(DEPOSITOR);
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
                .thenReturn(DEPOSITOR);
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
        return new DepositorCreate(NAMESPACE, ORGANIZATION, ADDRESS,
                ImmutableList.of(contactModel(valid)), ImmutableList.of());
    }

    private DepositorContactCreate contactModel(boolean valid) {
        if (valid) {
            return new DepositorContactCreate("test-name", EMAIL, new Phone("446681800", "CH"));
        } else {
            return new DepositorContactCreate("test-name", EMAIL, new Phone("0", "US"));
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