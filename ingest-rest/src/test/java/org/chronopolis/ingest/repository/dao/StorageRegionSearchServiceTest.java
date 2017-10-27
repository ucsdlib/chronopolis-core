package org.chronopolis.ingest.repository.dao;

import org.chronopolis.ingest.IngestTest;
import org.chronopolis.ingest.JpaContext;
import org.chronopolis.ingest.repository.NodeRepository;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.ingest.repository.criteria.StorageRegionSearchCriteria;
import org.chronopolis.rest.entities.storage.ReplicationConfig;
import org.chronopolis.rest.entities.storage.StorageRegion;
import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.persistence.EntityManager;

/**
 * Simple tests to validate querying and saving when interacting with {@link StorageRegion}s
 *
 * @author shake
 */
@RunWith(SpringRunner.class)
@DataJpaTest
@ContextConfiguration(classes = JpaContext.class)
public class StorageRegionSearchServiceTest extends IngestTest {

    private final Logger log = LoggerFactory.getLogger(StorageRegionSearchServiceTest.class);

    @Autowired private NodeRepository nodes;
    @Autowired private EntityManager entityManager;
    @Autowired private StorageRegionRepository repository;

    private StorageRegionService service;

    @Before
    public void setup() {
        service = new StorageRegionService(repository, entityManager);
    }

    @Test
    public void testPersist() {
        StorageRegion region = new StorageRegion();
        region.setDataType(DataType.BAG);
        region.setStorageType(StorageType.LOCAL);
        region.setCapacity(100L);
        region.setNode(nodes.findByUsername("umiacs"));

        ReplicationConfig config = new ReplicationConfig();
        config.setRegion(region);
        config.setUsername("test-user");
        config.setPath("test-path");
        config.setServer("test-server");
        region.setReplicationConfig(config);

        service.save(region);
        log.info("Persisted region with id {}", region.getId());

        StorageRegion persisted = service.find(new StorageRegionSearchCriteria().withId(region.getId()));

        Assert.assertNotNull("Persisted StorageRegion is null", persisted);
        Assert.assertNotNull("Persisted ReplicationConfig is null", persisted.getReplicationConfig());
        Assert.assertEquals(region.getId(), persisted.getId());
        Assert.assertEquals(region.getCapacity(), persisted.getCapacity());
        Assert.assertEquals(region.getStorageType(), persisted.getStorageType());
        Assert.assertEquals(region.getDataType(), persisted.getDataType());
        Assert.assertEquals(region.getReplicationConfig().getPath(), persisted.getReplicationConfig().getPath());
        Assert.assertEquals(region.getReplicationConfig().getUsername(), persisted.getReplicationConfig().getUsername());
        Assert.assertEquals(region.getReplicationConfig().getServer(), persisted.getReplicationConfig().getServer());
    }
}
