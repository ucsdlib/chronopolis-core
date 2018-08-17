package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.repair.Repair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 *
 * Created by shake on 1/24/17.
 */
public interface RepairRepository extends JpaRepository<Repair, Long>,
                                          QueryDslPredicateExecutor<Repair> {
}
