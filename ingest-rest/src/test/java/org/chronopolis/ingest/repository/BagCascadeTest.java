package org.chronopolis.ingest.repository;

import com.google.common.collect.ImmutableSet;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.criteria.BagSearchCriteria;
import org.chronopolis.ingest.repository.dao.BagService;
import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.entities.depositor.Depositor;
import org.chronopolis.rest.kot.entities.depositor.QDepositor;
import org.chronopolis.rest.kot.entities.storage.StagingStorage;
import org.chronopolis.rest.kot.entities.storage.StorageRegion;
import org.chronopolis.rest.kot.models.enums.BagStatus;
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

    private static final String TEST = "test";
    private static final String BAG_MERGE = "BAG_MERGE";
    private static final String BAG_PERSIST = "BAG_PERSIST";
    private static final String TOKEN_MERGE = "TOKEN_MERGE";
    private static final String TOKEN_PERSIST = "TOKEN_PERSIST";

    @Autowired private BagRepository bagRepository;
    @Autowired private StorageRegionRepository regions;
    @Autowired private EntityManager entityManager;

    private BagService bags;
    private JPAQueryFactory factory;

    @Before
    public void setup() {
        factory = new JPAQueryFactory(entityManager);
        bags = new BagService(bagRepository, entityManager);
    }

    @Test
    public void testBagStagingPersist() {
        Bag bag = createBag(BAG_PERSIST);
        StorageRegion region = regions.findOne(1L);
        bag.setBagStorage(ImmutableSet.of(new StagingStorage(region, 1L, 1L, TEST, true)));
        bags.save(bag);

        Bag persisted = bags.find(new BagSearchCriteria().withName(BAG_PERSIST));
        Assert.assertNotNull(persisted);
    }

    @Test
    public void testTokenStagingPersist() {
        Bag bag = createBag(TOKEN_PERSIST);
        StorageRegion region = regions.findOne(1L);
        bag.setTokenStorage(ImmutableSet.of(new StagingStorage(region, 1L, 1L, TEST, true)));
        bags.save(bag);

        Bag persisted = bags.find(new BagSearchCriteria().withName(TOKEN_PERSIST));
        Assert.assertNotNull(persisted);
    }

    @Test
    public void testBagStagingMerge() {
        StorageRegion region = regions.findOne(1L);
        Bag bag = bags.find(new BagSearchCriteria().withName(BAG_MERGE));
        Assert.assertNotNull(bag);

        bag.setBagStorage(ImmutableSet.of(new StagingStorage(region, 1L, 1L, TEST, true)));
        bags.save(bag);

        Bag merged = bags.find(new BagSearchCriteria().withName(BAG_MERGE));
        Assert.assertNotNull(merged);
        Assert.assertNotNull(merged.getBagStorage());
    }

    @Test
    public void testTokenStagingMerge() {
        StorageRegion region = regions.findOne(1L);
        Bag bag = bags.find(new BagSearchCriteria().withName(TOKEN_MERGE));
        Assert.assertNotNull(bag);

        bag.setTokenStorage(ImmutableSet.of(new StagingStorage(region, 1L, 1L, TEST, true)));
        bags.save(bag);

        Bag merged = bags.find(new BagSearchCriteria().withName(TOKEN_MERGE));
        Assert.assertNotNull(merged);
        Assert.assertNotNull(merged.getTokenStorage());
    }

    // helper
    public Bag createBag(String op) {
        Depositor depositor = factory.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq("test-depositor"))
                .fetchOne();

        assert depositor != null;
        // might need to init regions and what not
        return new Bag(op, TEST, depositor, 1L, 1L, BagStatus.DEPOSITED);
    }

}
