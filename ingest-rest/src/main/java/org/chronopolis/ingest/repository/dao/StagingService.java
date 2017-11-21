package org.chronopolis.ingest.repository.dao;

import com.querydsl.core.types.CollectionExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.StorageRepository;
import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.entities.storage.QFixity;
import org.chronopolis.rest.entities.storage.QStagingStorage;
import org.chronopolis.rest.entities.storage.StagingStorage;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Optional;

/**
 * Hey do some stuff look at me I'm a data access object woo
 *
 * @author shake
 */
@Transactional
public class StagingService extends SearchService<StagingStorage, Long, StorageRepository> {

    private final EntityManager manager;

    public StagingService(StorageRepository storageRepository, EntityManager manager) {
        super(storageRepository);
        this.manager = manager;
    }

    /**
     * Get the active storage object for a bag
     *
     * @param bag the bag whose staging_storage we're retrieving
     * @param storageJoin
     * @return the StagingStorage object, if found
     */
    public Optional<StagingStorage> activeStorageForBag(Bag bag, CollectionExpression<?, StagingStorage> storageJoin) {
        JPAQueryFactory factory = new JPAQueryFactory(manager);
        QBag b = QBag.bag;
        QStagingStorage storage = QStagingStorage.stagingStorage;
        // once again not the best query but that's ok
        return Optional.ofNullable(factory.from(b)
                .innerJoin(storageJoin, storage)
                .where(storage.active.isTrue().and(b.id.eq(bag.getId())))
                .select(storage)
                .fetchOne());
    }

    /**
     * Remove a fixity value fora given StagingStorage entity
     *
     * @param storage the StagingStorage entity
     * @param fixityId the id of the fixity to remove
     */
    public void deleteFixity(StagingStorage storage, Long fixityId) {
        JPAQueryFactory factory = new JPAQueryFactory(manager);
        QFixity fixity = QFixity.fixity;
        factory.delete(fixity)
                .where(fixity.storage.eq(storage), fixity.id.eq(fixityId))
                .execute();
    }
}
