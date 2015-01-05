package org.chronopolis.ingest.api;

import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.BagRepository;
import org.chronopolis.rest.models.Bag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@WebAppConfiguration
@IntegrationTest("server.port:0")
@DirtiesContext
public class StagingControllerTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    BagRepository bagRepository;

    @Before
    public void setup() {
        createBags();
    }

    /*
    @After
    public void teardown() {
        bagRepository.deleteAll();
    }
    */

    private void createBags() {
        Random r = new Random();
        for (int i = 0; i < 10; i++) {
            Bag b = new Bag("bag-" + i, "test-depositor");
            b.setFixityAlgorithm("SHA-256");
            b.setLocation("bags/test-bag-" + i);
            b.setSize(r.nextInt(50000));
            b.setTagManifestDigest("");
            b.setTokenDigest("");
            b.setTokenLocation("tokens/test-bag-" + i + "-tokens");
            bagRepository.save(b);
        }
    }

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
    }
}