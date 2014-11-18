package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.model.ReplicationAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Created by shake on 11/18/2014.
 */
public interface ReplicationRepository extends JpaRepository<ReplicationAction, Long> {

    Collection<ReplicationAction> findByNodeUsername(String username);
}
