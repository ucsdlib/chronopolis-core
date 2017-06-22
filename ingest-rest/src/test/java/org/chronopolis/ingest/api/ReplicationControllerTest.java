package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestSettings;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.WebContext;
import org.chronopolis.ingest.repository.criteria.SearchCriteria;
import org.chronopolis.ingest.repository.dao.ReplicationService;
import org.chronopolis.rest.entities.Replication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = WebContext.class)
@WebMvcTest(ReplicationController.class)
public class ReplicationControllerTest extends IngestTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ReplicationService service;

    @MockBean
    private IngestSettings settings;

    // @Test
    public void testCreateReplication() throws Exception {

    }

    // @Test
    public void testUpdateReplication() throws Exception {

    }

    @Test
    public void testReplications() throws Exception {
        when(service.findAll(any(SearchCriteria.class), any(Pageable.class))).thenReturn(null);
        mvc.perform(get("/api/replications?node=umiacs"))
                .andDo(print())
                .andExpect(status().is(200))
                .andReturn();

        // I guess we would want to verify as well
    }

    @Test
    public void testFindReplication() throws Exception {
        // Bag and Node should be defined
        when(service.find(any(SearchCriteria.class))).thenReturn(new Replication(null, null));
        mvc.perform(get("/api/replications/4").principal(() -> "user"))
                .andDo(print())
                .andExpect(status().is(200))
                .andReturn();
    }

    /*
    @Test
    public void testCorrectUpdate() throws Exception {
        TestRestTemplate template = getTemplate();
        ResponseEntity<Replication> entity;

        // two updates, one for tag and token
        template.put("http://localhost:" + port + "/api/replications/4/tokenstore", new FixityUpdate("token-fixity"));
        template.put("http://localhost:" + port + "/api/replications/4/tagmanifest", new FixityUpdate("tag-fixity"));
        entity = template.getForEntity("http://localhost:" + port + "/api/replications/4", Replication.class);
        Assert.assertEquals(ReplicationStatus.TRANSFERRED, entity.getBody().getStatus());
    }

    @Test
    public void testClientUpdates() throws Exception {
        TestRestTemplate template = getTemplate();
        ResponseEntity<Replication> entity;

        RStatusUpdate update = new RStatusUpdate(ReplicationStatus.STARTED);
        template.put("http://localhost:" + port + "/api/replications/4/status", update);
        entity = template.getForEntity("http://localhost:" + port + "/api/replications/4", Replication.class);
        Assert.assertEquals(ReplicationStatus.STARTED, entity.getBody().getStatus());
    }

    @Test
    public void testInvalidTagFixity() throws Exception {
        TestRestTemplate template = getTemplate();
        ResponseEntity<Replication> entity;

        template.put("http://localhost:" + port + "/api/replications/8/tagmanifest", new FixityUpdate("fxity"));
        entity = template.getForEntity("http://localhost:" + port + "/api/replications/8", Replication.class);
        Assert.assertEquals(ReplicationStatus.FAILURE_TAG_MANIFEST, entity.getBody().getStatus());
    }

    @Test
    public void testInvalidTokenFixity() throws Exception {
        TestRestTemplate template = getTemplate();
        ResponseEntity<Replication> entity;

        template.put("http://localhost:" + port + "/api/replications/8/tokenstore", new FixityUpdate("fxity"));
        entity = template.getForEntity("http://localhost:" + port + "/api/replications/8", Replication.class);
        Assert.assertEquals(ReplicationStatus.FAILURE_TOKEN_STORE, entity.getBody().getStatus());
    }


    public void testNonExistentReplication() throws Exception {
        ResponseEntity entity = getTemplate()
                .getForEntity("http://localhost:" + port + "/api/replications/12727", Object.class);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    public TestRestTemplate getTemplate() {
        ObjectMapper mapper = new ObjectMapper();
        builder.configure(mapper);
        List<HttpMessageConverter<?>> converters =
                ImmutableList.of(new MappingJackson2HttpMessageConverter(mapper));
        TestRestTemplate template = new TestRestTemplate("umiacs", "umiacs");
        // template.setMessageConverters(converters);
        return template;
    }
    */

}
