package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.storage.Storage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * Repository for Storage...s
 *
 * Created by shake on 7/11/17.
 */
public interface StorageRepository extends JpaRepository<Storage, Long>,
                                           QueryDslPredicateExecutor<Storage> {
}
