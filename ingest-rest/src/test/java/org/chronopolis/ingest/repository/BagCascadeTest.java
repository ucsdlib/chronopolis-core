package org.chronopolis.ingest.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.Depositor;
import org.chronopolis.rest.entities.QDepositor;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.BagStatus;
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
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

/**
 * @author shake
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SqlGroup(
        @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:sql/createDefaultStorageRegions.sql")
)
public class BagCascadeTest extends IngestTest {

    private final String TEST = "test";
    private final String BAG_MERGE = "BAG_MERGE";
    private final String BAG_PERSIST = "BAG_PERSIST";
    private final String TOKEN_MERGE = "TOKEN_MERGE";
    private final String TOKEN_PERSIST = "TOKEN_PERSIST";
    private JPAQueryFactory factory;

    @Autowired private BagRepository bagRepository;
    @Autowired private StorageRegionRepository regions;
    @Autowired private EntityManager entityManager;

    private BagService bags;

    @Before
    public void setup() {
        factory = new JPAQueryFactory(entityManager);
        bags = new BagService(bagRepository, entityManager);
    }

    @Test
    public void testBagStagingPersist() {
        Bag bag = createBag(BAG_PERSIST);
        bag.setBagStorage(new StagingStorage().setActive(true)
                .setRegion(regions.findOne(1L))
                .setPath(TEST)
                .setSize(1L)
                .setTotalFiles(1L));
        bags.save(bag);

        Bag persisted = bags.find(new BagSearchCriteria().withName(BAG_PERSIST));
        Assert.assertNotNull(persisted);
    }

    @Test
    public void testTokenStagingPersist() {
        Bag bag = createBag(TOKEN_PERSIST);
        StorageRegion region = regions.findOne(1L);
        bag.setTokenStorage(new StagingStorage().setActive(true)
                .setRegion(region)
                .setPath(TEST)
                .setSize(1L)
                .setTotalFiles(1L));
        bags.save(bag);

        Bag persisted = bags.find(new BagSearchCriteria().withName(TOKEN_PERSIST));
        Assert.assertNotNull(persisted);
    }

    @Test
    public void testBagStagingMerge() {
        Bag bag = bags.find(new BagSearchCriteria().withName(BAG_MERGE));
        Assert.assertNotNull(bag);

        bag.setBagStorage(new StagingStorage().setActive(true)
                .setRegion(regions.findOne(1L))
                .setPath(TEST)
                .setSize(1L)
                .setTotalFiles(1L));
        bags.save(bag);

        Bag merged = bags.find(new BagSearchCriteria().withName(BAG_MERGE));
        Assert.assertNotNull(merged);
        Assert.assertNotNull(merged.getBagStorage());
    }

    @Test
    public void testTokenStagingMerge() {
        Bag bag = bags.find(new BagSearchCriteria().withName(TOKEN_MERGE));
        Assert.assertNotNull(bag);

        bag.setTokenStorage(new StagingStorage().setActive(true)
                .setRegion(regions.findOne(1L))
                .setPath(TEST)
                .setSize(1L)
                .setTotalFiles(1L));
        bags.save(bag);

        Bag merged = bags.find(new BagSearchCriteria().withName(TOKEN_MERGE));
        Assert.assertNotNull(merged);
        Assert.assertNotNull(merged.getBagStorage());
    }

    // helper
    public Bag createBag(String op) {
        Depositor depositor = factory.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq("test-depositor"))
                .fetchOne();

        Bag bag = new Bag(op, depositor);
        bag.setCreator(TEST);
        bag.setSize(1L);
        bag.setStatus(BagStatus.DEPOSITED);
        bag.setTotalFiles(1L);
        bag.setRequiredReplications(1);
        return bag;
    }

}
