package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.StorageRegionService;
import org.chronopolis.rest.kot.entities.Node;
import org.chronopolis.rest.kot.models.create.RegionCreate;
import org.chronopolis.rest.kot.models.enums.DataType;
import org.chronopolis.rest.kot.models.enums.StorageType;
import org.chronopolis.rest.kot.models.enums.StorageUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;

import static com.google.common.collect.ImmutableSet.of;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * todo: tests for non-successful responses
 *
 * Created by shake on 7/11/17.
 */
@RunWith(SpringRunner.class)
@WebMvcTest(controllers = StorageController.class)
public class StorageControllerTest extends ControllerTest {

    private StorageController controller;

    // Constructor params
    @MockBean private NodeRepository nodes;
    @MockBean private StorageRegionService service;

    @Before
    public void setup() {
        controller = new StorageController(nodes, service);
        setupMvc(controller);
    }

    @Test
    public void getRegion() throws Exception {
        // todo: return actual StorageRegion and check json
        when(service.find(any(SearchCriteria.class))).thenReturn(null);

        mvc.perform(get("/api/storage/{id}", 1L)
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void getRegions() throws Exception {
        mvc.perform(get("/api/storage")
                .principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200));
    }

    @Test
    public void createRegion() throws Exception {
        authenticateUser();

        RegionCreate request = new RegionCreate("test-note",
                AUTHORIZED,
                1000L,
                DataType.BAG,
                StorageUnit.B,
                StorageType.LOCAL,
                "/test-path",
                "test-server",
                "test-user");

        when(nodes.findByUsername(eq(AUTHORIZED))).thenReturn(
                new Node(of(), of(), of(), AUTHORIZED, AUTHORIZED, true));
        mvc.perform(
                post("/api/storage")
                        .principal(authorizedPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(request)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

}