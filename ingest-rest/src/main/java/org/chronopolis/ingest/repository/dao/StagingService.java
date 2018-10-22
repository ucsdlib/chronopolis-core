package org.chronopolis.ingest.repository.dao;

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
@Deprecated
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
     * @param bag           the bag whose staging_storage we're retrieving
     * @param discriminator the discriminator to join on
     * @return the StagingStorage entity, if found
     */
    public Optional<StagingStorage> activeStorageForBag(Bag bag, String discriminator) {
        return activeStorageForBag(bag.getId(), discriminator);
    }

    /**
     * Get the active storage object for a bag given its id
     *
     * @param bag           the id of the bag
     * @param discriminator the discriminator to join on
     * @return the StagingStorage entity, if found
     */
    public Optional<StagingStorage> activeStorageForBag(Long bag, String discriminator) {
        JPAQueryFactory factory = new JPAQueryFactory(manager);
        QBag b = QBag.bag;
        QStagingStorage storage = QStagingStorage.stagingStorage;
        // once again not the best query but that's ok
        return Optional.ofNullable(factory.from(b)
                .innerJoin(b.storage, storage)
                .where(storage.active.isTrue()
                        .and(storage.file.dtype.eq(discriminator)
                        .and(storage.bag.id.eq(bag))))
                .select(storage)
                .fetchOne());
    }

    /**
     * Remove a fixity value fora given StagingStorage entity
     *
     * @param storage  the StagingStorage entity
     * @param fixityId the id of the fixity to remove
     */
    public void deleteFixity(StagingStorage storage, Long fixityId) {
        JPAQueryFactory factory = new JPAQueryFactory(manager);
        QFixity fixity = QFixity.fixity;
        factory.delete(fixity)
                .where(fixity.id.eq(fixityId))
                .execute();
    }
}
