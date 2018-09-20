package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.ReplicationService;
import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static org.chronopolis.ingest.repository.dao.StagingService.DISCRIMINATOR_BAG;
import static org.chronopolis.ingest.repository.dao.StagingService.DISCRIMINATOR_TOKEN;
import static org.chronopolis.rest.models.enums.FixityAlgorithm.SHA_256;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the replication controller
 * <p>
 * Things we need to setup before hand:
 * - Bag for the replication
 * - Node who is replicating
 * - Possibly node who does not own the replication
 * - SecurityContext (probably a better way to handle this but the static method usage kind of hurts us)
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = ReplicationController.class)
public class ReplicationControllerTest extends ControllerTest {

    private final String CORRECT_TAG_FIXITY = "tag-fixity";
    private final String CORRECT_TOKEN_FIXITY = "token-fixity";
    private final String INVALID_FIXITY = "fxity";
    private final Depositor depositor = new Depositor();

    private ReplicationController controller;

    @MockBean private StagingDao staging;
    @MockBean private ReplicationService service;

    @Before
    public void setup() {
        controller = new ReplicationController(staging, service);
        setupMvc(controller);
    }

    @Test
    public void testReplications() throws Exception {
        when(service.findAll(any(SearchCriteria.class), any(Pageable.class))).thenReturn(null);
        mvc.perform(get("/api/replications?node=umiacs").principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testFindReplication() throws Exception {
        Replication replication = new Replication(ReplicationStatus.PENDING,
                node(), bag(), "bag-url", "token-url", "protocol", null, null);
        when(service.find(any(SearchCriteria.class))).thenReturn(replication);
        mvc.perform(get("/api/replications/{id}", 4L).principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testCorrectUpdate() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(tokenStore(CORRECT_TOKEN_FIXITY))));
        setupPut("/api/replications/{id}/tokenstore", 4L, new FixityUpdate(CORRECT_TOKEN_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.receivedTokenFixity").value(CORRECT_TOKEN_FIXITY));
    }

    @Test
    public void testClientUpdate() throws Exception {
        setupPut("/api/replications/{id}/status", 4L,
                new ReplicationStatusUpdate(ReplicationStatus.STARTED))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    public void testInvalidTokenFixity() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(bagFile(CORRECT_TAG_FIXITY))));
        setupPut("/api/replications/{id}/tokenstore", 4L, new FixityUpdate(INVALID_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("FAILURE_TOKEN_STORE"))
                .andExpect(jsonPath("$.receivedTokenFixity").value(INVALID_FIXITY));
    }

    @Test
    public void testInvalidTagFixity() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_BAG)))
                .thenReturn(Optional.of(stagingStorage(tokenStore(CORRECT_TOKEN_FIXITY))));
        setupPut("/api/replications/{id}/tagmanifest", 4L, new FixityUpdate(INVALID_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("FAILURE_TAG_MANIFEST"))
                .andExpect(jsonPath("$.receivedTagFixity").value(INVALID_FIXITY));
    }

    public <T> ResultActions setupPut(String uri, Long id, T obj) throws Exception {
        Replication replication = new Replication(ReplicationStatus.PENDING,
                node(), bag(), "bag-url", "token-url", "protocol", null, null);
        when(service.find(any(SearchCriteria.class))).thenReturn(replication);
        authenticateUser();
        return mvc.perform(
                put(uri, id)
                        .with(user(user)) // any way to streamline some of this I wonder?
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(obj)));
                // .andDo(print());
    }

    private Bag bag() {
        StorageRegion region = new StorageRegion();
        region.setId(1L);

        Bag bag = new Bag("test-bag", "test-creator", depositor, 1L, 1L, BagStatus.REPLICATING);
        bag.setId(1L);
        bag.addStagingStorage(stagingStorage(bagFile(CORRECT_TAG_FIXITY)));
        bag.addStagingStorage(stagingStorage(tokenStore(CORRECT_TOKEN_FIXITY)));
        bag.setDistributions(new HashSet<>());
        return bag;
    }

    // not sure if we need to bother with id, filenames, etc on the Files
    private BagFile bagFile(String fixity) {
        BagFile bagFile = new BagFile();
        bagFile.addFixity(new Fixity(ZonedDateTime.now(), bagFile, fixity, SHA_256.getCanonical()));
        return bagFile;
    }

    private TokenStore tokenStore(String fixity) {
        TokenStore store = new TokenStore();
        store.addFixity(new Fixity(ZonedDateTime.now(), store, fixity, SHA_256.getCanonical()));
        return store;
    }

    private StagingStorage stagingStorage(DataFile file) {
        StagingStorage storage = new StagingStorage();
        storage.setFile(file);
        return storage;
    }


    private Node node() {
        return new Node(of(), "user", "password", true);
    }

}
