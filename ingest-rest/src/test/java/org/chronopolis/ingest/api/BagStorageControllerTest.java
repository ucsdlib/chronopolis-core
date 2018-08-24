package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.dao.StagingService;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.FixityAlgorithm;
import org.chronopolis.rest.models.update.ActiveToggle;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.ZonedDateTime;
import java.util.Optional;

import static java.util.Collections.emptySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the BagStorageController
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BagStorageController.class)
public class BagStorageControllerTest extends ControllerTest {

    private static final long ID = 1L;
    private static final String TYPE = "bag";

    private StagingStorage storage;
    private BagStorageController controller;

    @MockBean
    private StagingService stagingService;

    @Before
    public void setup() {
        StorageRegion region = new StorageRegion();
        region.setId(ID);
        region.setCapacity(100000L);
        region.setNode(new Node(emptySet(), AUTHORIZED, AUTHORIZED, true));

        Fixity fixity = new Fixity();
        fixity.setAlgorithm(FixityAlgorithm.SHA_256.getCanonical());
        fixity.setCreatedAt(ZonedDateTime.now());
        fixity.setValue("test-value");

        BagFile file = new BagFile();
        file.setId(ID);
        file.setSize(10L);
        file.setDtype("BAG");
        file.setFilename("test-path");
        file.getFixities().add(fixity);

        storage = new StagingStorage();
        storage.setId(ID);
        storage.setFile(file);
        storage.setSize(100L);
        storage.setActive(true);
        storage.setRegion(region);
        storage.setTotalFiles(10L);
        storage.setPath("test-path");
        storage.setCreatedAt(ZonedDateTime.now());
        storage.setUpdatedAt(ZonedDateTime.now());

        controller = new BagStorageController(stagingService);
        setupMvc(controller);
    }

    @Test
    public void testGetStorage() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(TYPE)))
                .thenReturn(Optional.of(storage));
        mvc.perform(get("/api/bags/{id}/storage/{type}", ID, TYPE))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.size").value(100L));
    }

    @Test
    public void testUpdateStorage() throws Exception {
        authenticateUser();
        ActiveToggle toggle = new ActiveToggle(false);
        when(stagingService.activeStorageForBag(eq(ID), eq(TYPE)))
                .thenReturn(Optional.of(storage));
        mvc.perform(
                put("/api/bags/{id}/storage/{type}", ID, TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(toggle))
                        .principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    public void testGetFixities() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(TYPE)))
                .thenReturn(Optional.of(storage));
        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity", ID, TYPE))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetFixity() throws Exception {
        when(stagingService.activeStorageForBag(eq(ID), eq(TYPE)))
                .thenReturn(Optional.of(storage));

        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity/{alg}", ID, TYPE, "SHA-256"))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testAddFixity() throws Exception {
        authenticateUser();
        when(stagingService.activeStorageForBag(eq(ID), eq(TYPE)))
                .thenReturn(Optional.of(storage));
        mvc.perform(put("/api/bags/{id}/storage/{type}/fixity", ID, TYPE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"algorithm\": \"test-put\", \"value\": \"success\"}")
                .principal(authorizedPrincipal))
                // .andDo(print())
                .andExpect(status().is(HttpStatus.CREATED.value()));
    }


}