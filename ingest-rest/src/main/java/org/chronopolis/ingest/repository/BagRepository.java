package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Bag;
import org.chronopolis.rest.models.enums.BagStatus;
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

}
