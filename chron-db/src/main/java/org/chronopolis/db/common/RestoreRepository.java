package org.chronopolis.db.common;

import org.chronopolis.db.common.model.RestoreRequest;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by shake on 8/8/14.
 */
@Transactional
public interface RestoreRepository extends CrudRepository<RestoreRequest, String> {

    RestoreRequest findByCorrelationId(String correlationId);

}
