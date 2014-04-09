package org.chronopolis.db.ingest;

import org.chronopolis.db.model.CollectionIngest;
import org.springframework.data.repository.CrudRepository;

/**
 * Created by shake on 4/9/14.
 */
public interface IngestDB extends CrudRepository<CollectionIngest, String> {

    CollectionIngest findByCorrelationId(String correlationId);

}
