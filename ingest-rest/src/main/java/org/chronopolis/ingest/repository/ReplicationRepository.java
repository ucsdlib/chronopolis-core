package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Replication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

/**
 * Repository for interacting with {@link Replication} requests
 *
 * Created by shake on 11/18/2014.
 */
public interface ReplicationRepository extends JpaRepository<Replication, Long>,
                                               QueryDslPredicateExecutor<Replication> {


}
