package org.chronopolis.ingest.api;

import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = RestoreController.class)
public class RestoreControllerTest extends ControllerTest {

    private RestoreController controller;

    @MockBean private RestoreRepository restores;
    @MockBean private NodeRepository nodes;

    @Before
    public void setup() {
        controller = new RestoreController(restores, nodes);
        setupMvc(controller);
    }

    @Test
    @Ignore
    public void testGetRestorations() throws Exception {
        // when(restores.findByStatus(any(ReplicationStatus.class))).thenReturn(null);
        mvc.perform(
                get("/api/restorations")
                        .principal(authorizedPrincipal))
                .andExpect(status().is(200));
    }

    // @Test
    public void testPutRestoration() throws Exception {
    }

    @Test
    @Ignore
    public void testGetRestoration() throws Exception {
        // Restoration restoration = new Restoration("depositor", "restore-1", "some-link");
        // restoration.setNode(new Node("user", "password"));
        // when(restores.findOne(anyLong())).thenReturn(restoration);
        mvc.perform(
                get("/api/restorations/{id}", 1L)
                        .principal(authorizedPrincipal))
                .andExpect(status().is(200));
    }

    // @Test
    public void testGetRestorationNotExists() throws Exception {
    }

    // @Test
    public void testUpdateRestoration() throws Exception {
    }
}