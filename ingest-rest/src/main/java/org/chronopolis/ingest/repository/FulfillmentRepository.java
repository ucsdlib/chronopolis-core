package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Fulfillment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 *
 * Created by shake on 1/24/17.
 */
public interface FulfillmentRepository extends JpaRepository<Fulfillment, Long>,
                                               QueryDslPredicateExecutor<Fulfillment> {
}
