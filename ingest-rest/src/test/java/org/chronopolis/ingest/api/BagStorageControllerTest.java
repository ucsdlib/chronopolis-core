package org.chronopolis.ingest.api;

import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.time.ZonedDateTime;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the BagStorageController
 *
 * todo: do we want to have proper objectmapping in the MockMvc (i.e. entity -> model)
 *
 */
@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = BagStorageController.class)
@ContextConfiguration(classes = WebContext.class)
public class BagStorageControllerTest {
    private static final long ID = 1L;
    private static final String TYPE = "bag";

    private Bag bag;

    @Autowired
    private MockMvc mvc;

    @MockBean private NodeRepository nodes;
    @MockBean private SearchService<Bag, Long, BagRepository> bagService;

    @Before
    public void setup() {
        StorageRegion region = new StorageRegion();
        region.setId(ID);
        region.setCapacity(100000L);

        Fixity fixity = new Fixity();
        fixity.setAlgorithm("test-algorithm")
              .setCreatedAt(ZonedDateTime.now())
              .setValue("test-value");

        StagingStorage storage = new StagingStorage();
        storage.setId(ID);
        storage.setActive(true);
        storage.setPath("test-path");
        storage.setSize(100L);
        storage.setTotalFiles(10L);
        storage.setRegion(region);
        storage.addFixity(fixity);
        storage.setCreatedAt(ZonedDateTime.now());
        storage.setUpdatedAt(ZonedDateTime.now());

        bag = new Bag("test-bag", "test-depositor");
        bag.setBagStorage(storage);
    }

    @Test
    public void testGetStorage() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);

        mvc.perform(get("/api/bags/{id}/storage/{type}", ID, TYPE))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.id").value(ID))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.size").value(100L));
    }

    @Test
    public void testUpdateStorage() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);

        mvc.perform(put("/api/bags/{id}/storage/{type}", ID, TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\": false}"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    public void testGetFixities() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);
        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity", ID, TYPE))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testGetFixity() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);
        mvc.perform(get("/api/bags/{id}/storage/{type}/fixity/{alg}", ID, TYPE, "test-algorithm"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()));
    }

    @Test
    public void testAddFixity() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag);
        mvc.perform(put("/api/bags/{id}/storage/{type}/fixity", ID, TYPE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"algorithm\": \"test-put\", \"value\": \"success\"}"))
                .andDo(print())
                .andExpect(status().is(HttpStatus.CREATED.value()));
    }


}