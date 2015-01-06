package org.chronopolis.ingest.repository;

import org.chronopolis.rest.models.Replication;
import org.chronopolis.rest.models.ReplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Created by shake on 11/18/2014.
 */
public interface ReplicationRepository extends JpaRepository<Replication, Long> {

    Collection<Replication> findByStatusAndNodeUsername(ReplicationStatus status, String username);

    Collection<Replication> findByNodeUsername(String username);

    Replication findByNodeUsernameAndBagID(String username, Long id);

}
