package org.chronopolis.ingest.api;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.TestApplication;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    // @Test
    public void testCreateReplication() throws Exception {

    }

    // @Test
    public void testUpdateReplication() throws Exception {

    }

    @Test
    public void testReplications() throws Exception {
        ResponseEntity<List> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/staging/replications", List.class);

        assertEquals(2, entity.getBody().size());
    }

    @Test
    public void testFindReplication() throws Exception {
        // as we have 4 nodes and umiacs is the last, we know the 4th replication will be for umiacs
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/staging/replications/4", Object.class);


        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void testUnauthorizedGetReplication() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/staging/replications/3", Object.class);

        assertEquals(HttpStatus.UNAUTHORIZED, entity.getStatusCode());
    }

    public void testNonExistentReplication() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/staging/replications/12727", Object.class);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

}