package org.chronopolis.ingest.repository;

import org.chronopolis.rest.kot.entities.storage.StagingStorage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * Repository for Storage...s
 *
 * Created by shake on 7/11/17.
 */
public interface StorageRepository extends JpaRepository<StagingStorage, Long>,
                                           QueryDslPredicateExecutor<StagingStorage> {
}
