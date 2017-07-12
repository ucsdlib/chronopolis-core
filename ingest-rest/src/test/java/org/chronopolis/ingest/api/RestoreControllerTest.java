package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.RestoreRepository;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.Restoration;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(secure = false, controllers = RestoreController.class)
@ContextConfiguration(classes = WebContext.class)
public class RestoreControllerTest extends IngestTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RestoreRepository restores;

    @MockBean
    private NodeRepository nodes;

    @Test
    public void testGetRestorations() throws Exception {
        when(restores.findByStatus(any(ReplicationStatus.class))).thenReturn(null);
        mvc.perform(
                get("/api/restorations")
                        .principal(() -> "user"))
                .andExpect(status().is(200));
    }

    // @Test
    public void testPutRestoration() throws Exception {
    }

    @Test
    public void testGetRestoration() throws Exception {
        Restoration restoration = new Restoration("depositor", "restore-1", "some-link");
        restoration.setNode(new Node("user", "password"));
        when(restores.findOne(anyLong())).thenReturn(restoration);
        mvc.perform(
                get("/api/restorations/{id}", 1L)
                        .principal(() -> "user"))
                .andExpect(status().is(200));
    }

    // @Test
    public void testGetRestorationNotExists() throws Exception {
    }

    // @Test
    public void testUpdateRestoration() throws Exception {
    }
}