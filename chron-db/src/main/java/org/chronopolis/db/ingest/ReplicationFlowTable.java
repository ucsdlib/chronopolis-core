package org.chronopolis.db.ingest;

import org.chronopolis.db.model.ReplicationFlow;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by shake on 6/12/14.
 */
@Transactional
public interface ReplicationFlowTable extends CrudRepository<ReplicationFlow, Long> {

    ReplicationFlow findByDepositorAndCollectionAndNode(String depositor, String collection, String node);

    ReplicationFlow findByNodeAndCorrelationId(String node, String correlationId);

}
