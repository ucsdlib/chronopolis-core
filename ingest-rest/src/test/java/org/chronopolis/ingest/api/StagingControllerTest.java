package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.rest.models.Bag;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;


/**
 * Because of the application.properties, we don't need to load any bags here.
 * It is done through the DevConfig class
 *
 */

@WebIntegrationTest("server.port:0")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createBags.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteBags.sql")
})
public class StagingControllerTest extends IngestTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    BagRepository bagRepository;

    @Test
    public void testGetBags() throws Exception {
        ResponseEntity<List> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags", List.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(10, entity.getBody().size());
    }

    @Test
    public void testGetBag() throws Exception {
        ResponseEntity<Bag> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags/1", Bag.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("bag-0", entity.getBody().getName());
    }

    @Test
    public void testNonExistentBag() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/bags/12015851", Object.class);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    // @Test
    public void testStageBag() throws Exception {
        // TODO: Actual staging involves creating tokens - is there any way to get around this?
        // slash is that something we want to test?
        // maybe just test that we don't try to restage any bags
    }
}