package org.chronopolis.ingest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.support.PageImpl;
import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.FixityUpdate;
import org.chronopolis.rest.models.RStatusUpdate;
import org.chronopolis.rest.models.ReplicationStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@WebIntegrationTest("server.port:0")
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createReplications.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteReplications.sql")
})
public class ReplicationControllerTest extends IngestTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    Jackson2ObjectMapperBuilder builder;

    // @Test
    public void testCreateReplication() throws Exception {

    }

    // @Test
    public void testUpdateReplication() throws Exception {

    }

    @Test
    public void testReplications() throws Exception {
        ResponseEntity<PageImpl> entity = getTemplate()
                .getForEntity("http://localhost:" + port + "/api/replications?node=umiacs", PageImpl.class);

        assertEquals(2, entity.getBody().getTotalElements());
    }

    @Test
    public void testFindReplication() throws Exception {
        // Replication Id of 4 is defined in the SQL
        ResponseEntity<Replication> entity = getTemplate()
                .getForEntity("http://localhost:" + port + "/api/replications/4", Replication.class);

        System.out.println(entity.getBody());

        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void testCorrectUpdate() throws Exception {
        TestRestTemplate template = getTemplate();
        ResponseEntity<Replication> entity;

        // two updates, one for tag and token
        template.put("http://localhost:" + port + "/api/replications/4/tokenstore", new FixityUpdate("fixity"));
        template.put("http://localhost:" + port + "/api/replications/4/tagmanifest", new FixityUpdate("fixity"));
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
        template.setMessageConverters(converters);
        return template;
    }

}