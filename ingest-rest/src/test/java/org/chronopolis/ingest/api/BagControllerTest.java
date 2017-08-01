package org.chronopolis.ingest.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.SearchService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.models.IngestRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.any;
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
@WebMvcTest(secure = false, controllers = BagController.class)
@ContextConfiguration(classes = WebContext.class)
public class BagControllerTest extends IngestTest {

    private final String DEPOSITOR = "test-depositor";
    private final String BAG = "test-bag";
    private final String LOCATION = "bags/test-bag-0";
    private final String NODE = "test-node";

    @Autowired
    private MockMvc mvc;

    // Mocks for the StagingController
    @MockBean
    private SearchService<Bag, Long, BagRepository> bagService;

    @MockBean
    private NodeRepository nodes;

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
        IngestRequest request = new IngestRequest();
        request.setDepositor(DEPOSITOR);
        request.setSize(1L);
        request.setTotalFiles(1L);
        request.setStorageRegion(1L);
        request.setName(BAG);
        request.setLocation(LOCATION);
        request.setReplicatingNodes(ImmutableList.of(NODE));

        when(bagService.find(any(SearchCriteria.class))).thenReturn(null);
        when(nodes.findByUsername(eq(NODE))).thenReturn(new Node(NODE, "password"));

        mvc.perform(
                post("/api/bags")
                    .principal(() -> "user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJson(request)))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.depositor").value(DEPOSITOR))
                .andExpect(jsonPath("$.name").value(BAG));
                // todo: expect storage values
                // .andExpect(jsonPath("$.location").value(LOCATION))
                // .andExpect(jsonPath("$.fixityAlgorithm").value("SHA-256"));

        verify(bagService, times(1)).find(any(SearchCriteria.class));
        verify(nodes, times(1)).findByUsername(eq(NODE));
    }

    @Test
    public void testStageExists() throws Exception {
        IngestRequest request = new IngestRequest();
        request.setDepositor(DEPOSITOR);
        request.setName(BAG);
        request.setLocation(LOCATION);
        request.setReplicatingNodes(ImmutableList.of(NODE));

        when(bagService.find(any(SearchCriteria.class))).thenReturn(bag());

        mvc.perform(
                post("/api/bags")
                    .principal(() -> "user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(asJson(request)))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.depositor").value(DEPOSITOR))
                .andExpect(jsonPath("$.name").value(BAG));

        verify(bagService, times(1)).find(any(SearchCriteria.class));
        verify(nodes, times(0)).findByUsername(eq(NODE));
    }

    private Bag bag() {
        Bag b = new Bag(BAG, DEPOSITOR);
        b.setId(1L);
        return b;
    }

    private <T> String asJson(T request) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


}