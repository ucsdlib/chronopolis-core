package org.chronopolis.db.replication;

import org.chronopolis.db.replication.model.RestoreRequest;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by shake on 8/8/14.
 */
public interface RestoreRepository extends CrudRepository<RestoreRequest, String> {
}
