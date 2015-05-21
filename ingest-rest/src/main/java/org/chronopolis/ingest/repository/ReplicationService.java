package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.rest.models.Replication;
import org.springframework.beans.factory.annotation.Autowired;
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
            setPredicate(o, predicate, criteriaMap.get(o));
        }

        return replicationRepository.findOne(predicate);
    }

    public void setPredicate(Object criteria, BooleanExpression predicate, BooleanExpression expression) {
        if (criteria != null) {
            predicate = setExpression(predicate, expression);
        }
    }

}
