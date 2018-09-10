package org.chronopolis.rest.entities;

import com.google.common.collect.ImmutableSet;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
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
import java.time.ZonedDateTime;
import java.util.HashSet;

import static org.chronopolis.rest.entities.JPAContext.FIXITY_ALGORITHM;
import static org.chronopolis.rest.entities.JPAContext.FIXITY_VALUE;

/**
 * Oh boy
 *
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class BagEntityTest {

    private final Long LONG_VALUE = 1L;
    private final String CREATOR = "bag-entity-test";
    private final String TEST_PATH = "/test-path";

    @Autowired
    private EntityManager entityManager;

    private Node ncar;
    private Node umiacs;
    private Depositor depositor;
    private StorageRegion storageRegion;

    @Before
    @SuppressWarnings("Duplicates")
    public void initFromDb() {
        Assert.assertNotNull(entityManager);

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        storageRegion = qf.selectFrom(QStorageRegion.storageRegion)
                .where(QStorageRegion.storageRegion.id.eq(1L))
                .fetchOne();
        depositor = qf.selectFrom(QDepositor.depositor)
                .where(QDepositor.depositor.namespace.eq("test-depositor"))
                .fetchOne();
        ncar = qf.selectFrom(QNode.node)
                .where(QNode.node.username.eq("ncar"))
                .fetchOne();
        umiacs = qf.selectFrom(QNode.node)
                .where(QNode.node.username.eq("umiacs"))
                .fetchOne();

        Assert.assertNotNull(storageRegion);
        Assert.assertNotNull(depositor);
    }

    /**
     * Test persistence of a Bag with all its underlying relations
     * <p>
     * - Bag
     * -- File
     * ---- Fixity
     * -- Storage (w/ File)
     * -- Distribution
     */
    @Test
    public void testBagPersistTests() {
        final String BAG_NAME = "test-bag-persist";

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        Bag persist = new Bag();
        persist.setName(BAG_NAME);
        persist.setCreator(CREATOR);
        persist.setSize(LONG_VALUE);
        persist.setDepositor(depositor);
        persist.setTotalFiles(LONG_VALUE);
        persist.setStatus(BagStatus.DEPOSITED);

        BagFile bf = new BagFile();
        Fixity fixity = new Fixity(ZonedDateTime.now(), bf, FIXITY_VALUE, FIXITY_ALGORITHM);
        bf.setFilename(TEST_PATH);
        bf.setSize(LONG_VALUE);
        bf.setBag(persist);
        bf.addFixity(fixity);

        TokenStore ts = new TokenStore();
        ts.setFilename(TEST_PATH + "-token");
        ts.setSize(LONG_VALUE);
        ts.setBag(persist);

        persist.addFiles(ImmutableSet.of(bf, ts));

        StagingStorage bagStore =
                new StagingStorage(storageRegion, persist, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        StagingStorage tokenStore =
                new StagingStorage(storageRegion, persist, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        bagStore.setFile(bf);
        tokenStore.setFile(ts);
        persist.setStorage(ImmutableSet.of(bagStore, tokenStore));
        persist.setDistributions(new HashSet<>());
        persist.addDistribution(ncar, BagDistributionStatus.DISTRIBUTE);
        persist.addDistribution(umiacs, BagDistributionStatus.DEGRADED);

        entityManager.persist(persist);

        Bag fetch = qf.selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(BAG_NAME))
                .fetchOne();

        Assert.assertNotNull(fetch);
        Assert.assertEquals(persist, fetch);
        Assert.assertNotEquals(0, persist.getId());
        Assert.assertEquals(2, fetch.getStorage().size());
        Assert.assertEquals(2, fetch.getDistributions().size());

        // also storage
        StagingStorage fetchStorage = qf.select(QStagingStorage.stagingStorage)
                .from(QBag.bag)
                .join(QBag.bag.storage, QStagingStorage.stagingStorage)
                .where(QStagingStorage.stagingStorage.active.isTrue()
                    .and(QStagingStorage.stagingStorage.file.dtype.eq("BAG")))
                .fetchOne();

        // basically just check that it's the BagFile's data
        Assert.assertNotNull(fetchStorage);
        Assert.assertNotNull(fetchStorage.getFile());
        Assert.assertEquals(TEST_PATH, fetchStorage.getFile().getFilename());
    }

    /**
     * Test persist of a Bag followed by a merge for its relations
     * <p>
     * Bag
     * - File
     * -- Fixity
     * - Storage
     * - Distribution
     */
    @Test
    public void testBagMergeTests() {
        final String BAG_NAME = "test-bag-merge";

        JPAQueryFactory qf = new JPAQueryFactory(entityManager);
        Bag bag = new Bag();
        // set basic fields which need to be init
        // leave out bagStorage, tokenStorage, and distributions even though they're lateinits
        // in order to test if we can persist without setting them (we can as long as we refresh)
        bag.setName(BAG_NAME);
        bag.setCreator(CREATOR);
        bag.setSize(LONG_VALUE);
        bag.setDepositor(depositor);
        bag.setTotalFiles(LONG_VALUE);
        bag.setStatus(BagStatus.DEPOSITED);

        // persist + refresh just in case
        entityManager.persist(bag);
        entityManager.refresh(bag);

        BagFile bagFile = new BagFile();
        Fixity fixity = new Fixity(ZonedDateTime.now(), bagFile, FIXITY_VALUE, FIXITY_ALGORITHM);
        bagFile.setFilename(TEST_PATH);
        bagFile.setSize(LONG_VALUE);
        bagFile.setBag(bag);
        bagFile.addFixity(fixity);

        TokenStore tokenFile = new TokenStore();
        tokenFile.setFilename(TEST_PATH + "-token");
        tokenFile.setSize(LONG_VALUE);
        tokenFile.setBag(bag);

        bag.getFiles().add(bagFile);
        bag.getFiles().add(tokenFile);

        // setup Staging entities to merge
        StagingStorage bagStore =
                new StagingStorage(storageRegion, bag, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        StagingStorage bagStoreInactive =
                new StagingStorage(storageRegion, bag, LONG_VALUE, LONG_VALUE, TEST_PATH, false);
        StagingStorage tokenStore =
                new StagingStorage(storageRegion, bag, LONG_VALUE, LONG_VALUE, TEST_PATH, true);
        bagStore.setFile(bagFile);
        bagStoreInactive.setFile(bagFile);
        tokenStore.setFile(tokenFile);

        bag.getStorage().add(bagStore);
        bag.getStorage().add(bagStoreInactive);
        bag.getStorage().add(tokenStore);
        bag.addDistribution(ncar, BagDistributionStatus.REPLICATE);
        bag.addDistribution(umiacs, BagDistributionStatus.DISTRIBUTE);
        entityManager.merge(bag);

        // fetch and asserts
        Bag fetchedBag = qf.selectFrom(QBag.bag)
                .where(QBag.bag.name.eq(BAG_NAME))
                .fetchOne();

        Assert.assertNotEquals(0L, bag.getId());
        Assert.assertNotNull(fetchedBag);
        Assert.assertEquals(bag, fetchedBag);
        Assert.assertEquals(3, fetchedBag.getStorage().size());
        Assert.assertEquals(2, fetchedBag.getDistributions().size());
        Assert.assertEquals(2, fetchedBag.getFiles().size());
    }

}
