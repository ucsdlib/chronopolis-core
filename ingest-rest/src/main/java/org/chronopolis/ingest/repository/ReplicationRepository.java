package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.model.Replication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Created by shake on 11/18/2014.
 */
public interface ReplicationRepository extends JpaRepository<Replication, Long> {

    Collection<Replication> findByNodeUsername(String username);

    Replication findByNodeUsernameAndBagID(String username, Long id);

}
