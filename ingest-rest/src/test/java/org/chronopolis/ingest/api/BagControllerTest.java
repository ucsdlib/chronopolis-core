package org.chronopolis.ingest.api;

import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.ingest.repository.dao.StorageRegionService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.IngestRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Tests for the staging API
 *
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = BagController.class)
public class BagControllerTest extends ControllerTest {

    private final String DEPOSITOR = "test-depositor";
    private final String BAG = "test-bag";
    private final String LOCATION = "bags/test-bag-0";
    private final String NODE = "test-node";

    private BagController controller;

    // Mocks for the StagingController
    @MockBean private NodeRepository nodes;
    @MockBean private BagService bagService;
    @MockBean private StorageRegionService regions;

    @Before
    public void setup() {
        controller = new BagController(nodes, bagService, regions);
        setupMvc(controller);
    }

    @Test
    public void testGetBags() throws Exception {
        // todo actual return value
        // todo correct search criteria
        when(bagService.findAll(any(SearchCriteria.class), any(Pageable.class))).thenReturn(null);

        mvc.perform(
                get("/api/bags/")
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void testGetBag() throws Exception {
        // todo actual return value
        // todo correct search criteria
        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag());

        mvc.perform(
                get("/api/bags/{id}", 1L)
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.depositor").value("test-depositor"))
                .andExpect(jsonPath("$.name").value("test-bag"));
    }

    @Test
    public void testGetDoesNotExist() throws Exception {
        when(bagService.find(any(SearchCriteria.class))).thenReturn(null);
        mvc.perform(
                get("/api/bags/{id}", 100L)
                        .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(404));
    }

    @Test
    public void testStageBag() throws Exception {
        // Bag Ingest Request
        IngestRequest request = new IngestRequest();
        request.setDepositor(DEPOSITOR);
        request.setSize(1L);
        request.setTotalFiles(1L);
        request.setStorageRegion(1L);
        request.setName(BAG);
        request.setLocation(LOCATION);
        request.setReplicatingNodes(ImmutableList.of(NODE));

        // created bag to return
        Bag bag = bag();
        bag.setName(BAG);
        bag.setDepositor(DEPOSITOR);

        when(regions.find(any(SearchCriteria.class))).thenReturn(new StorageRegion());
        when(nodes.findByUsername(eq(NODE))).thenReturn(new Node(NODE, "password"));
        when(bagService.create(eq("user"), eq(request), any(StorageRegion.class), any(Set.class))).thenReturn(bag);

        mvc.perform(
                post("/api/bags")
                    .principal(() -> "user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJson(request)))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.depositor").value(DEPOSITOR))
                .andExpect(jsonPath("$.name").value(BAG));

        // verify(bagService, times(1)).find(any(SearchCriteria.class));
        verify(bagService, times(1)).create(eq("user"), eq(request), any(StorageRegion.class), anySet());
        verify(nodes, times(1)).findByUsername(eq(NODE));
    }

    private Bag bag() {
        Bag b = new Bag(BAG, DEPOSITOR);
        b.setId(1L);
        return b;
    }

}