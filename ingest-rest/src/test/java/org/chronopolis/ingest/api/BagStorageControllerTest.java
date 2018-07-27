package org.chronopolis.ingest.api;

import com.querydsl.core.types.dsl.SetPath;
import org.chronopolis.ingest.repository.dao.StagingService;
import org.chronopolis.rest.kot.entities.Node;
import org.chronopolis.rest.kot.entities.QBag;
import org.chronopolis.rest.kot.entities.storage.Fixity;
import org.chronopolis.rest.kot.entities.storage.QStagingStorage;
import org.chronopolis.rest.kot.entities.storage.StagingStorage;
import org.chronopolis.rest.kot.entities.storage.StorageRegion;
import org.chronopolis.rest.kot.models.update.ActiveToggle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Optional;

import static java.util.Collections.emptySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the BagStorageController
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BagStorageController.class)
public class BagStorageControllerTest extends ControllerTest {

    private final Logger log = LoggerFactory.getLogger(BagStorageControllerTest.class);

    private static final long ID = 1L;
    private static final String TYPE = "bag";

    private StagingStorage storage;
    private BagStorageController controller;
    private SetPath<StagingStorage, QStagingStorage> storageJoin;

    @MockBean
    private StagingService stagingService;

    @Before
    public void setup() {
        storageJoin = QBag.bag.bagStorage;
        StorageRegion region = new StorageRegion();
        region.setId(ID);
        region.setCapacity(100000L);
        region.setNode(new Node(emptySet(), emptySet(), emptySet(), AUTHORIZED, AUTHORIZED, true));

        Fixity fixity = new Fixity();
        fixity.setAlgorithm("test-algorithm");
        fixity.setCreatedAt(ZonedDateTime.now());
        fixity.setValue("test-value");

        storage = new StagingStorage();
        storage.setId(ID);
        storage.setActive(true);
        storage.setPath("test-path");
        storage.setSize(100L);
        storage.setTotalFiles(10L);
        storage.setRegion(region);
        storage.setFixities(new HashSet<>());
        storage.getFixities().add(fixity);
        storage.setCreatedAt(ZonedDateTime.now());
        storage.setUpdatedAt(ZonedDateTime.now());

        controller = new BagStorageController(stagingService);
        setupMvc(controller);
    }

    @Test
    public void testGetStorage() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(storageJoin)))
                .thenReturn(Optional.of(storage));
        mvc.perform(get("/api/bags/{id}/storage/{type}", ID, TYPE))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.size").value(100L));
    }

    @Test
    public void testUpdateStorage() throws Exception {
        authenticateUser();
        ActiveToggle toggle = new ActiveToggle(false);
        when(stagingService.activeStorageForBag(eq(ID), eq(storageJoin)))
                .thenReturn(Optional.of(storage));
        mvc.perform(
                put("/api/bags/{id}/storage/{type}", ID, TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(toggle))
                        .principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    public void testGetFixities() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(storageJoin)))
                .thenReturn(Optional.of(storage));
        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity", ID, TYPE))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetFixity() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(storageJoin))).thenReturn(Optional.of(storage));
        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity/{alg}", ID, TYPE, "test-algorithm"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testAddFixity() throws Exception {
        authenticateUser();
        when(stagingService.activeStorageForBag(eq(ID), eq(storageJoin))).thenReturn(Optional.of(storage));
        mvc.perform(put("/api/bags/{id}/storage/{type}/fixity", ID, TYPE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"algorithm\": \"test-put\", \"value\": \"success\"}")
                .principal(authorizedPrincipal))
                .andDo(print())
                .andExpect(status().is(HttpStatus.CREATED.value()));
    }


}