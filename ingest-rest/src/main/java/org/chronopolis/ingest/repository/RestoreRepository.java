package org.chronopolis.ingest.repository;

import org.chronopolis.rest.entities.Restoration;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for interacting with {@link Restoration}s
 *
 * Created by shake on 12/8/14.
 */
@Deprecated
public interface RestoreRepository extends JpaRepository<Restoration, Long> {

}
