package org.chronopolis.ingest.repository.dao;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.chronopolis.ingest.repository.StorageRepository;
import org.chronopolis.rest.entities.storage.QFixity;
import org.chronopolis.rest.entities.storage.StagingStorage;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;

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
