package org.chronopolis.rest.entities.storage;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.rest.entities.JPAContext;
import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.QNode;
import org.chronopolis.rest.models.enums.DataType;
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

/**
 * @author shake
 */
@DataJpaTest
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = JPAContext.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class StorageRegionEntityTest {

    @Autowired
    private EntityManager entityManager;
    private Node ucsd;

    @Before
    public void initFromDb() {
        JPAQueryFactory query = new JPAQueryFactory(entityManager);

        ucsd = query.selectFrom(QNode.node)
                .where(QNode.node.username.eq("ucsd"))
                .fetchOne();

        Assert.assertNotNull(ucsd);
    }

    @Test
    public void persistStorageRegion() {
        StorageRegion persist = new StorageRegion();
        persist.setCapacity(10000L);
        persist.setNode(ucsd);
        persist.setDataType(DataType.BAG);
        persist.setNote("test-persist-storage-region");

        ReplicationConfig config = new ReplicationConfig(persist, "test-path", "test-server", "test-user");
        persist.setReplicationConfig(config);

        entityManager.persist(persist);

        JPAQueryFactory query = new JPAQueryFactory(entityManager);
        StorageRegion fetch = query.selectFrom(QStorageRegion.storageRegion)
                .where(QStorageRegion.storageRegion.note.eq("test-persist-storage-region"))
                .fetchOne();

        // basic persist tests
        Assert.assertNotNull(fetch);
        Assert.assertNotEquals(0L, persist.getId());
        Assert.assertEquals(persist, fetch);

        // test fetch worked properly
        Assert.assertNotNull(fetch.getReplicationConfig());
        Assert.assertEquals("test-path", fetch.getReplicationConfig().getPath());
        Assert.assertEquals("test-server", fetch.getReplicationConfig().getServer());
        Assert.assertEquals("test-user", fetch.getReplicationConfig().getUsername());
    }

}
