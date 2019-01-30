package org.chronopolis.ingest.support;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestProperties;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDao;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.QBagFile;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.chronopolis.ingest.JpaContext.CREATE_SCRIPT;
import static org.chronopolis.ingest.JpaContext.DELETE_SCRIPT;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD;
import static org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD;

@DataJpaTest(excludeAutoConfiguration = FlywayAutoConfiguration.class)
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = BEFORE_TEST_METHOD, scripts = CREATE_SCRIPT),
        @Sql(executionPhase = AFTER_TEST_METHOD, scripts = DELETE_SCRIPT)
})
public class BagFileCSVProcessorTest extends IngestTest {

    @Autowired
    private EntityManager entityManager;

    private final IngestProperties properties = new IngestProperties();

    @Test
    public void testReadValidCsv() throws URISyntaxException {
        final String BAG_NAME = "bag-2";
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");

        PagedDao dao = new PagedDao(entityManager);
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(BAG_NAME));
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("valid.csv");

        BagFileCSVProcessor processor = new BagFileCSVProcessor(dao, properties);
        ResponseEntity response = processor.apply(bag.getId(), toCsv);

        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        long count = factory.selectFrom(QBagFile.bagFile)
                .where(QBagFile.bagFile.bag.name.eq(BAG_NAME))
                .fetchCount();

        // manifest + tagmanifest + 8 data files = 10
        Assert.assertEquals(10, count);
        Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void testReadInvalidCsv() throws URISyntaxException {
        final String BAG_NAME = "bag-2";
        final URL csvRoot = ClassLoader.getSystemClassLoader().getResource("csv");

        PagedDao dao = new PagedDao(entityManager);
        Bag bag = dao.findOne(QBag.bag, QBag.bag.name.eq(BAG_NAME));
        Path toCsv = Paths.get(csvRoot.toURI()).resolve("invalid.csv");

        BagFileCSVProcessor processor = new BagFileCSVProcessor(dao, properties);
        ResponseEntity response = processor.apply(bag.getId(), toCsv);
        Assert.assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }

}