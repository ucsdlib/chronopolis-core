package org.chronopolis.rest.entities;

import com.google.common.collect.ImmutableSet;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.depositor.Depositor;
import org.chronopolis.rest.entities.depositor.QDepositor;
import org.chronopolis.rest.entities.storage.QStorageRegion;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.enums.BagStatus;
import org.chronopolis.rest.models.enums.ReplicationStatus;
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

import static org.chronopolis.rest.entities.BagDistributionStatus.DISTRIBUTE;
import static org.chronopolis.rest.entities.BagDistributionStatus.REPLICATE;

/**
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class ReplicationEntityTest {

    private final Long LONG_VALUE = 1L;
    private final String PROTOCOL = "test-protocol";
    private final String CREATOR_NAME = "replication-entity-test";

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

    @Test
    public void testReplicationPersist() {
        final String BAG_LINK = "persist-bag-link";
        final String TOKEN_LINK = "persist-token-link";
        final String BAG_NAME = "test-replication-persist";

        Bag bag = new Bag();
        bag.setDistributions(ImmutableSet.of(new BagDistribution(bag, ncar, DISTRIBUTE)));
        bag.setStatus(BagStatus.REPLICATING);
        bag.setName(BAG_NAME);
        bag.setSize(LONG_VALUE);
        bag.setDepositor(depositor);
        bag.setCreator(CREATOR_NAME);
        bag.setTotalFiles(LONG_VALUE);

        entityManager.persist(bag);

        Replication persist = new Replication();
        persist.setBag(bag);
        persist.setNode(ncar);
        persist.setBagLink(BAG_LINK);
        persist.setProtocol(PROTOCOL);
        persist.setTokenLink(TOKEN_LINK);

        entityManager.persist(persist);
        entityManager.refresh(persist);
        entityManager.flush();

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        Replication fetch = query.selectFrom(QReplication.replication)
                .where(QReplication.replication.id.eq(persist.getId()))
                .fetchOne();

        // persist check
        Assert.assertNotNull(fetch);
        Assert.assertEquals(persist, fetch);

        // make sure these are actually null as they have not yet been set
        Assert.assertNull(fetch.getReceivedTagFixity());
        Assert.assertNull(fetch.getReceivedTokenFixity());
    }

    /**
     * I wonder if the tests could be ordered such that we don't need to persist a bunch of entities
     * again. Not really a big deal and probably better to keep tests idempotent anyhow.
     */
    @Test
    public void testAllReplicatedUpdatesBag() {
        final String BAG_LINK = "merge-bag-link";
        final String TOKEN_LINK = "merge-token-link";
        final String BAG_FIXITY = "merge-bag-fixity";
        final String TOKEN_FIXITY = "merge-token-fixity";
        final String BAG_NAME = "test-replication-update";

        Bag bag = new Bag();
        bag.setDistributions(new HashSet<>());
        bag.addDistribution(new BagDistribution(bag, ncar, DISTRIBUTE));
        bag.addDistribution(new BagDistribution(bag, umiacs, REPLICATE));
        bag.setStatus(BagStatus.REPLICATING);
        bag.setName(BAG_NAME);
        bag.setSize(LONG_VALUE);
        bag.setDepositor(depositor);
        bag.setCreator(CREATOR_NAME);
        bag.setTotalFiles(LONG_VALUE);

        entityManager.persist(bag);

        Replication merge = new Replication();
        merge.setBag(bag);
        merge.setNode(ncar);
        merge.setBagLink(BAG_LINK);
        merge.setProtocol(PROTOCOL);
        merge.setTokenLink(TOKEN_LINK);

        entityManager.persist(merge);
        entityManager.refresh(merge);

        merge.setReceivedTagFixity(BAG_FIXITY);
        merge.setReceivedTokenFixity(TOKEN_FIXITY);
        merge.setStatus(ReplicationStatus.SUCCESS);
        entityManager.merge(merge);

        entityManager.refresh(bag);
        entityManager.flush();
        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        Replication fetch = query.selectFrom(QReplication.replication)
                .where(QReplication.replication.id.eq(merge.getId()))
                .fetchOne();


        // typical check everything persisted correctly
        Assert.assertNotNull(fetch);
        Assert.assertEquals(merge, fetch);

        // update occurred
        Assert.assertEquals(BAG_FIXITY, fetch.getReceivedTagFixity());
        Assert.assertEquals(TOKEN_FIXITY, fetch.getReceivedTokenFixity());
        Assert.assertNotEquals(bag.getCreatedAt(), fetch.getUpdatedAt());

        // update cascaded to other entities
        bag.getDistributions().forEach(dist -> Assert.assertEquals(REPLICATE, dist.getStatus()));
        Assert.assertEquals(BagStatus.PRESERVED, bag.getStatus());
    }

}
