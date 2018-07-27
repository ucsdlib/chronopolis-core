package org.chronopolis.ingest.repository;

import org.chronopolis.rest.kot.entities.Bag;
import org.chronopolis.rest.kot.models.enums.BagStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.Collection;

/**
 * Repository for interacting with {@link Bag}s
 *
 * Created by shake on 11/6/14.
 */
public interface BagRepository extends JpaRepository<Bag, Long>,
                                       QueryDslPredicateExecutor<Bag> {

    Collection<Bag> findByStatus(BagStatus status);
    Page<Bag> findByStatus(BagStatus status, Pageable pageable);

}
