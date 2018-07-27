package org.chronopolis.ingest.tokens;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.dao.PagedDAO;
import org.chronopolis.rest.kot.entities.QBag;
import org.chronopolis.rest.kot.entities.serializers.ExtensionsKt;
import org.chronopolis.rest.kot.models.Bag;
import org.chronopolis.tokenize.ManifestEntry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.persistence.EntityManager;

@DataJpaTest
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup({
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
                scripts = "classpath:sql/createBagsWithTokens.sql"),
        @Sql(executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD,
                scripts = "classpath:sql/deleteBagsWithTokens.sql")
})
public class DatabasePredicateTest extends IngestTest {

    @Autowired
    private EntityManager entityManager;

    private PagedDAO dao;
    private DatabasePredicate predicate;

    @Before
    public void setup() {
        dao = new PagedDAO(entityManager);
        predicate = new DatabasePredicate(dao);
    }

    @Test
    public void test() {
        final String digest = "test-digest";
        final String fileExists = "/data/hello_world";
        final String fileNotExists = "/data/hello_other_world";
        org.chronopolis.rest.kot.entities.Bag be = dao.findOne(QBag.bag, QBag.bag.name.eq("new-bag-3"));

        final Bag bag = ExtensionsKt.model(be);
        // an unfortunate side effect of immutability + java :(
        final Bag invalidId = bag.copy(999L,
                bag.getSize(),
                bag.getTotalFiles(),
                bag.getBagStorage(),
                bag.getTokenStorage(),
                bag.getCreatedAt(),
                bag.getUpdatedAt(),
                bag.getName(),
                bag.getCreator(),
                bag.getDepositor(),
                bag.getStatus(),
                bag.getReplicatingNodes());

        /* None of this can be tested while the ME relies on the old version...
        ManifestEntry exists = new ManifestEntry(bag, fileExists, digest);
        ManifestEntry bagNotExists = new ManifestEntry(invalidId, fileNotExists, digest);
        ManifestEntry tokenNotExists = new ManifestEntry(bag, fileNotExists, digest);
        */

        /*
        Assert.assertFalse(predicate.test(exists));
        Assert.assertFalse(predicate.test(bagNotExists));
        Assert.assertTrue(predicate.test(tokenNotExists));
        */
    }

    @Test
    public void testNullChecks() {
        Assert.assertFalse(predicate.test(null));
        Assert.assertFalse(predicate.test(new ManifestEntry(null, "file", "test-digest")));
    }
}