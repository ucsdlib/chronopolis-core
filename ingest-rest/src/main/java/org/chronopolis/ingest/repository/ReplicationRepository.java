package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;

import java.util.Collection;

/**
 * Repository for interacting with {@link Replication} requests
 *
 * Created by shake on 11/18/2014.
 */
public interface ReplicationRepository extends JpaRepository<Replication, Long>,
                                               QueryDslPredicateExecutor<Replication> {

    Collection<Replication> findByStatusAndNodeUsername(ReplicationStatus status, String username);
    Page<Replication> findByStatusAndNodeUsername(ReplicationStatus status, String username, Pageable pageable);

    Collection<Replication> findByNodeUsername(String username);
    Page<Replication> findByNodeUsername(String username, Pageable pageable);

    // Replication findByNodeUsernameAndBagId(String username, Long id);

}
