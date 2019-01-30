package org.chronopolis.ingest.api;

import com.querydsl.core.types.Predicate;
import org.chronopolis.ingest.repository.dao.ReplicationDao;
import org.chronopolis.ingest.repository.dao.StagingDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.DataFile;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.entities.TokenStore;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.projections.CompleteBag;
import org.chronopolis.rest.entities.projections.ReplicationView;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
import org.chronopolis.rest.models.update.FixityUpdate;
import org.chronopolis.rest.models.update.ReplicationStatusUpdate;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashSet;
import java.util.Optional;

import static com.google.common.collect.ImmutableSet.of;
import static java.time.ZonedDateTime.now;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_BAG;
import static org.chronopolis.ingest.repository.dao.StagingDao.DISCRIMINATOR_TOKEN;
import static org.chronopolis.rest.models.enums.FixityAlgorithm.SHA_256;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private final Long ID = 4L;
    private static final String PUT_STATUS = "/api/replications/{id}/status";
    private static final String PUT_TAG_FIXITY = "/api/replications/{id}/tagmanifest";
    private static final String GET_REPLICATION = "/api/replications/{id}";
    private static final String GET_REPLICATIONS = "/api/replications?node=umiacs";
    private static final String PUT_TOKEN_FIXITY = "/api/replications/{id}/tokenstore";
    private final String CORRECT_TAG_FIXITY = "tag-fixity";
    private final String CORRECT_TOKEN_FIXITY = "token-fixity";
    private final String INVALID_FIXITY = "invalid-fixity";
    private final Depositor depositor = new Depositor();

    @MockBean private StagingDao staging;
    @MockBean private ReplicationDao replicationDao;

    @Before
    public void setup() {
        ReplicationController controller = new ReplicationController(staging, replicationDao);
        setupMvc(controller);
    }

    @Test
    public void testReplications() throws Exception {
        when(replicationDao.findAll(any(), any())).thenReturn(null);
        mvc.perform(get(GET_REPLICATIONS).principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testFindReplication() throws Exception {
        CompleteBag bag = new CompleteBag(ID, NAMESPACE, NAMESPACE, ID, ID, BagStatus.REPLICATING,
                now(), now(), NAMESPACE, emptySet(), emptyMap());
        ReplicationView view = new ReplicationView(ID, now(), now(), ReplicationStatus.TRANSFERRED,
                "link", "link", "protocol", "fixity", "fixity", NAMESPACE, bag);
        when(replicationDao.findReplicationAsView(eq(ID))).thenReturn(view);
        mvc.perform(get(GET_REPLICATION, ID).principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
    }

    @Test
    public void testFixityComplete() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_BAG)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(tokenStore())));
        setupPut(PUT_TAG_FIXITY, ID, CORRECT_TAG_FIXITY, CORRECT_TOKEN_FIXITY,
                new FixityUpdate(CORRECT_TAG_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("TRANSFERRED"))
                .andExpect(jsonPath("$.receivedTokenFixity").value(CORRECT_TOKEN_FIXITY));
    }

    @Test
    public void testCorrectUpdate() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_BAG)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(tokenStore())));
        setupPut(PUT_TOKEN_FIXITY, ID, null, null, new FixityUpdate(CORRECT_TOKEN_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.receivedTokenFixity").value(CORRECT_TOKEN_FIXITY));
    }

    @Test
    public void testClientUpdate() throws Exception {
        setupPut(PUT_STATUS, ID, null, null, new ReplicationStatusUpdate(ReplicationStatus.STARTED))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("STARTED"));
    }

    @Test
    public void testInvalidTokenFixity() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_BAG)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        setupPut(PUT_TOKEN_FIXITY, ID, null, null, new FixityUpdate(INVALID_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("FAILURE_TOKEN_STORE"))
                .andExpect(jsonPath("$.receivedTokenFixity").value(INVALID_FIXITY));
    }

    @Test
    public void testInvalidTagFixity() throws Exception {
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_BAG)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        when(staging.activeStorageForBag(any(Bag.class), eq(DISCRIMINATOR_TOKEN)))
                .thenReturn(Optional.of(stagingStorage(bagFile())));
        setupPut(PUT_TAG_FIXITY, ID, null, null, new FixityUpdate(INVALID_FIXITY))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.status").value("FAILURE_TAG_MANIFEST"))
                .andExpect(jsonPath("$.receivedTagFixity").value(INVALID_FIXITY));
    }

    private <T> ResultActions setupPut(String uri,
                                       Long id,
                                       @Nullable String tagFixity,
                                       @Nullable String tokenFixity,
                                       T obj) throws Exception {
        Replication replication = replication(tagFixity, tokenFixity);
        when(replicationDao.findOne(any(), any(Predicate.class))).thenReturn(replication);
        authenticateUser();
        return mvc.perform(put(uri, id)
                .with(user(user))
                .principal(authorizedPrincipal)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJson(obj)));
    }

    private Replication replication(String tagFixity, String tokenFixity) {
        return new Replication(ReplicationStatus.PENDING, node(), bag(), "bag-url", "token-url",
                "protocol", tagFixity, tokenFixity);
    }

    private Bag bag() {
        StorageRegion region = new StorageRegion();
        region.setId(1L);

        Bag bag = new Bag("test-bag", "test-creator", depositor, 1L, 1L, BagStatus.REPLICATING);
        bag.setId(1L);
        bag.addStagingStorage(stagingStorage(bagFile()));
        bag.addStagingStorage(stagingStorage(tokenStore()));
        bag.setDistributions(new HashSet<>());
        return bag;
    }

    // not sure if we need to bother with id, filenames, etc on the Files
    private BagFile bagFile() {
        BagFile bagFile = new BagFile();
        bagFile.addFixity(new Fixity(now(), bagFile, CORRECT_TAG_FIXITY, SHA_256.getCanonical()));
        return bagFile;
    }

    private TokenStore tokenStore() {
        TokenStore store = new TokenStore();
        store.addFixity(new Fixity(now(), store, CORRECT_TOKEN_FIXITY, SHA_256.getCanonical()));
        return store;
    }

    private StagingStorage stagingStorage(DataFile file) {
        StagingStorage storage = new StagingStorage();
        storage.setFile(file);
        return storage;
    }

    private Node node() {
        return new Node(of(), AUTHORIZED, AUTHORIZED, true);
    }

}
