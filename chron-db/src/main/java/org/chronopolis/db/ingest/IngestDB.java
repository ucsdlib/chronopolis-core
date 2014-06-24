package org.chronopolis.db.ingest;

import org.chronopolis.db.model.CollectionIngest;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

/**
 * Created by shake on 4/9/14.
 */
@Transactional
public interface IngestDB extends CrudRepository<CollectionIngest, String> {

    CollectionIngest findByCorrelationId(String correlationId);

    CollectionIngest findByNameAndDepositor(String name, String depositor);

}
