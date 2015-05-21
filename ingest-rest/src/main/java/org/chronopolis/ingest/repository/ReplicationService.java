package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.rest.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Map;

import static org.chronopolis.ingest.repository.PredicateUtil.setExpression;

/**
 * Class to help querying for replication objects based on various values.
 * ex: search by node, bag-id, status
 *
 * Created by shake on 5/21/15.
 */
@Component
@Transactional
public class ReplicationService {
    private final Logger log = LoggerFactory.getLogger(ReplicationService.class);

    @Autowired
    ReplicationRepository replicationRepository;

    @Autowired
    BagRepository bagRepository;

    @Autowired
    NodeRepository nodeRepository;

    public Replication getReplication(ReplicationSearchCriteria criteria) {
        BooleanExpression predicate = null;

        Map<Object, BooleanExpression> criteriaMap = criteria.getCriteria();
        for (Object o : criteriaMap.keySet()) {
            predicate = setExpression(predicate, criteriaMap.get(o));
        }

        return replicationRepository.findOne(predicate);
    }

    public Page<Replication> getReplications(ReplicationSearchCriteria criteria, Pageable pageable) {
        BooleanExpression predicate = null;

        Map<Object, BooleanExpression> criteriaMap = criteria.getCriteria();
        for (Object o : criteriaMap.keySet()) {
            predicate = setExpression(predicate, criteriaMap.get(o));
        }

        if (predicate == null) {
            log.trace("No predicate, returning all replications");
            return replicationRepository.findAll(pageable);
        }

        log.trace("Returning replications which satisfy the predicate");
        return replicationRepository.findAll(predicate, pageable);
    }

    public void save(Replication replication) {
        replicationRepository.save(replication);
    }

}
