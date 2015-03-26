package org.chronopolis.ingest.api;

import org.chronopolis.ingest.TestApplication;
import org.chronopolis.ingest.repository.RestoreRepository;
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

@WebIntegrationTest("server.port:0")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = TestApplication.class)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createRestorations.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD, scripts = "classpath:sql/deleteRestorations.sql")
})
public class RestoreControllerTest {

    @Value("${local.server.port}")
    private int port;

    @Autowired
    RestoreRepository repository;

    @Test
    public void testGetRestorations() throws Exception {
        ResponseEntity<List> entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/restorations", List.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals(1, entity.getBody().size());
    }

    @Test
    public void testPutRestoration() throws Exception {

    }

    @Test
    public void testGetRestoration() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/restorations/1", Object.class);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    public void testGetRestorationNotExists() throws Exception {
        ResponseEntity entity = new TestRestTemplate("umiacs", "umiacs")
                .getForEntity("http://localhost:" + port + "/api/restorations/1789124", Object.class);

        assertEquals(HttpStatus.NOT_FOUND, entity.getStatusCode());
    }

    // @Test
    public void testUpdateRestoration() throws Exception {

    }
}