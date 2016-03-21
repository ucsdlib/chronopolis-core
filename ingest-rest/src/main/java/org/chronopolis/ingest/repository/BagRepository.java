package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.BagStatus;
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

    // Bag findById(Long id);
    Bag findByNameAndDepositor(String name, String depositor);

    Collection<Bag> findByStatus(BagStatus status);
    Page<Bag> findByStatus(BagStatus status, Pageable pageable);

}
