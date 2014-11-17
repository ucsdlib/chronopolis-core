package org.chronopolis.ingest.repository;

import org.chronopolis.ingest.model.Node;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Created by shake on 11/17/14.
 */
public interface NodeRepository extends JpaRepository<Node, Long> {

}
