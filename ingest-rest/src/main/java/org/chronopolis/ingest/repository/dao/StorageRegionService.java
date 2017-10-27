package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.StorageRegionRepository;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.StorageRegion;

import javax.persistence.EntityManager;
import java.util.Optional;

/**
 * hey look another guy here just saying remember to eat your vegetables so you can
 * grow up to be a strong and healthy dao just like me
 *
 * @author shake
 */
public class StorageRegionService extends SearchService<StorageRegion, Long, StorageRegionRepository> {

    private final EntityManager entityManager;

    public StorageRegionService(StorageRegionRepository storageRegionRepository, EntityManager entityManager) {
        super(storageRegionRepository);
        this.entityManager = entityManager;
    }

    /**
     * Get the amount of space used by a StorageRegion
     *
     * @param region the storage region
     * @return the used space
     */
    public Optional<Long> getUsedSpace(StorageRegion region) {
        JPAQueryFactory factory = new JPAQueryFactory(entityManager);
        QStagingStorage storage = QStagingStorage.stagingStorage;
        return Optional.ofNullable(factory.selectFrom(storage)
                .select(storage.size.sum())
                // make sure this queries on region.id
                .where(storage.region.eq(region), storage.active.isTrue())
                .fetchOne());
    }
}
