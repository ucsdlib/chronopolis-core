package org.chronopolis.rest.entities.repair;

import com.google.common.collect.ImmutableSet;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.BagDistributionStatus;
import org.chronopolis.rest.entities.JPAContext;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.FulfillmentType;
import org.chronopolis.rest.models.enums.RepairStatus;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;
import java.util.HashSet;

/**
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class RepairTest {

    private final String CREATOR = "creator";
    private final String TEST_FILE_0 = "/bagit.txt";
    private final String TEST_FILE_1 = "/bag-info.txt";

    @Autowired
    private EntityManager entityManager;

    private Node to;
    private Node from;
    private Depositor depositor;
    private StorageRegion storageRegion;

    @Before
    public void init() {
        to = entityManager.find(Node.class, 1L);
        from = entityManager.find(Node.class, 2L);

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        storageRegion = qf.selectFrom(QStorageRegion.storageRegion)
                .where(QStorageRegion.storageRegion.id.eq(1L))
                .fetchOne();
        depositor = qf.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq("test-depositor"))
                .fetchOne();

        Assert.assertNotNull(to);
        Assert.assertNotNull(from);
    }

    @Test
    public void persistRepair() {
        final String name = "test-repair-persist";

        Bag bag = createBag(name);
        Repair repair = new Repair(bag, to, null,  // from node, null initial val
                RepairStatus.REQUESTED, AuditStatus.PRE,
                null, null, // fulfillment type, null initial
                CREATOR,
                false, false, false);
        repair.setFiles(new HashSet<>());
        repair.addFilesFromRequest(ImmutableSet.of(TEST_FILE_0, TEST_FILE_1));

        entityManager.persist(repair);

        // and pull the repair yea
        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        Repair fetch = qf.selectFrom(QRepair.repair)
                .where(QRepair.repair.bag.name.eq(name))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertEquals(2, fetch.files.size());
        Assert.assertEquals(repair.getTo(), fetch.getTo());
        Assert.assertEquals(repair.getRequester(), fetch.getRequester());

        Assert.assertNull(repair.getFrom());
        Assert.assertNull(repair.getType());
        Assert.assertNull(repair.getStrategy());

    }

    @Test
    public void mergeStrategyAce() {
        final String url = "test-url";
        final String name = "test-repair-ace";
        final String apiKey = "test-api-key";

        Bag bag = createBag(name);
        Repair repair = new Repair(bag, to, null,  // from node, null initial val
                RepairStatus.REQUESTED, AuditStatus.PRE,
                null, null, // fulfillment type, null initial
                CREATOR,
                false, false, false);
        repair.setFiles(new HashSet<>());
        repair.addFilesFromRequest(ImmutableSet.of(TEST_FILE_0, TEST_FILE_1));

        entityManager.persist(repair);

        Strategy strategy = new Ace(apiKey, url);
        strategy.setRepair(repair);
        repair.setFrom(from);
        repair.setStrategy(strategy);
        repair.setType(FulfillmentType.ACE);

        entityManager.merge(repair);

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        Repair fetch = qf.selectFrom(QRepair.repair)
                .where(QRepair.repair.bag.name.eq(name))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertNotNull(fetch.getFrom());
        Assert.assertNotNull(fetch.getStrategy());
        Assert.assertNotNull(fetch.getType());
        Assert.assertEquals(from, fetch.getFrom());
        Assert.assertEquals(FulfillmentType.ACE, fetch.getType());
        Assert.assertEquals(url, ((Ace) fetch.getStrategy()).getUrl());
        Assert.assertEquals(apiKey, ((Ace) fetch.getStrategy()).getApiKey());
    }

    @Test
    public void mergeStrategyRsync() {
        final String link = "test-link";
        final String name = "test-repair-rsync";

        Bag bag = createBag(name);
        Repair repair = new Repair(bag, to, null,  // from node, null initial val
                RepairStatus.REQUESTED, AuditStatus.PRE,
                null, null, // fulfillment type, null initial
                CREATOR,
                false, false, false);
        repair.setFiles(new HashSet<>());
        repair.addFilesFromRequest(ImmutableSet.of(TEST_FILE_0, TEST_FILE_1));

        entityManager.persist(repair);

        Strategy strategy = new Rsync(link);
        strategy.setRepair(repair);
        repair.setFrom(from);
        repair.setStrategy(strategy);
        repair.setType(FulfillmentType.NODE_TO_NODE);

        entityManager.merge(repair);

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        Repair fetch = qf.selectFrom(QRepair.repair)
                .where(QRepair.repair.bag.name.eq(name))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertNotNull(fetch.getFrom());
        Assert.assertNotNull(fetch.getStrategy());
        Assert.assertNotNull(fetch.getType());
        Assert.assertEquals(from, fetch.getFrom());
        Assert.assertEquals(FulfillmentType.NODE_TO_NODE, fetch.getType());
        Assert.assertEquals(link, ((Rsync) fetch.getStrategy()).getLink());
    }

    private Bag createBag(String name) {
        Bag persist = new Bag();
        persist.setName(name);
        String CREATOR = "bag-entity-test";
        persist.setCreator(CREATOR);
        // push this into another class maybe
        Long LONG_VALUE = 1L;
        persist.setSize(LONG_VALUE);
        persist.setDepositor(depositor);
        persist.setTotalFiles(LONG_VALUE);
        persist.setStatus(BagStatus.DEPOSITED);

        String TEST_PATH = "test-path";
        StagingStorage bagStore =
                new StagingStorage(storageRegion, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        StagingStorage tokenStore =
                new StagingStorage(storageRegion, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        persist.setBagStorage(ImmutableSet.of(bagStore));
        persist.setTokenStorage(ImmutableSet.of(tokenStore));
        persist.setDistributions(new HashSet<>());
        persist.addDistribution(from, BagDistributionStatus.DISTRIBUTE);
        persist.addDistribution(to, BagDistributionStatus.DEGRADED);
        entityManager.persist(persist);
        return persist;
    }
}
