package org.chronopolis.ingest.repository;

import org.chronopolis.rest.kot.entities.Node;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for interacting with {@link Node}s
 *
 * Created by shake on 11/17/14.
 */
public interface NodeRepository extends JpaRepository<Node, Long> {

    Node findByUsername(String username);

}
