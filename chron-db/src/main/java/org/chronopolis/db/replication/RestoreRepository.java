package org.chronopolis.db.replication;

import org.chronopolis.db.replication.model.RestoreRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by shake on 8/8/14.
 */
@Transactional
public interface RestoreRepository extends CrudRepository<RestoreRequest, String> {

    RestoreRequest findByCorrelationId(String correlationId);

}
