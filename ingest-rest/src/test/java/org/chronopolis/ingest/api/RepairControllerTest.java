package org.chronopolis.ingest.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.FulfillmentRepository;
import org.chronopolis.ingest.repository.RepairRepository;
import org.chronopolis.ingest.repository.RepairService;
import org.chronopolis.ingest.repository.SearchService;
import org.chronopolis.ingest.support.PageImpl;
import org.chronopolis.rest.models.repair.Fulfillment;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.Repair;
import org.chronopolis.rest.models.repair.RepairStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Tests for our repair controller
 *
 * Created by shake on 1/26/17.
 */
@WebIntegrationTest("server.port:0")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createRepairs.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteRepairs.sql")
})
public class RepairControllerTest extends IngestTest {
    private final Logger log = LoggerFactory.getLogger(RepairControllerTest.class);

    @Value("${local.server.port}")
    private int port;

    @Autowired
    Jackson2ObjectMapperBuilder builder;

    @Autowired
    private RepairService<org.chronopolis.rest.entities.Repair, Long, RepairRepository> repairs;

    @Autowired
    private SearchService<org.chronopolis.rest.entities.Fulfillment, Long, FulfillmentRepository> fulfillments;

    @Test
    public void getRepair() throws Exception {
        ResponseEntity<Repair> entity = getTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/repair/requests/" + 1, Repair.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        Repair repair = entity.getBody();
        assertEquals(RepairStatus.FULFILLING, repair.getStatus());
        assertEquals("ucsd", repair.getRequester());
    }

    @Test
    public void getRepairs() throws Exception {
        ResponseEntity<PageImpl> entity = getTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/repair/requests", PageImpl.class);

        log.info(entity.toString());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(1, entity.getBody().getTotalElements());
    }

    @Test
    public void getFulfillment() {
        ResponseEntity<Fulfillment> entity = getTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/repair/fulfillments/" + 1, Fulfillment.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        Fulfillment fulfillment = entity.getBody();
        assertEquals("umiacs", fulfillment.getFrom());
        assertEquals(FulfillmentStatus.STAGING, fulfillment.getStatus());
    }

    @Test
    public void getFulfillments() {
        ResponseEntity<PageImpl> entity = getTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/repair/fulfillments", PageImpl.class);

        log.info(entity.toString());
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(1, entity.getBody().getTotalElements());
    }


    // @Test
    public void createRequest() throws Exception {

    }

    // @Test
    public void fulfillRequest() throws Exception {

    }

    // Basically the same as in the ReplicationController test, should move to IngestTest
    public TestRestTemplate getTemplate(String user, String pass) {
        ObjectMapper mapper = new ObjectMapper();
        builder.configure(mapper);
        List<HttpMessageConverter<?>> converters =
                ImmutableList.of(new MappingJackson2HttpMessageConverter(mapper));
        TestRestTemplate template = new TestRestTemplate(user, pass);
        template.setMessageConverters(converters);
        return template;
    }
}