package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.storage.StorageRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * Repository for StorageRegions
 *
 * Created by shake on 7/11/17.
 */
public interface StorageRegionRepository extends JpaRepository<StorageRegion, Long>,
                                                 QueryDslPredicateExecutor<StorageRegion> {
}
