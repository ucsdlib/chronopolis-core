package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.models.ReplicationStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Search criteria for Replication transfers
 *
 * TODO: If we have the map, do we need the fields?
 *
 * Created by shake on 5/21/15.
 */
public class ReplicationSearchCriteria {
    private QReplication replication;

    private Map<Object, BooleanExpression> criteria;

    public ReplicationSearchCriteria() {
        replication = QReplication.replication;
        criteria = new HashMap<>();
    }

    public ReplicationSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put("id", replication.id.eq(id));
        }
        return this;
    }

    public ReplicationSearchCriteria withBagId(Long bagId) {
        if (bagId != null) {
            criteria.put("BAG_ID", replication.bag.id.eq(bagId));
        }
        return this;
    }

    public ReplicationSearchCriteria withNodeUsername(String nodeUsername) {
        if (nodeUsername != null && !nodeUsername.isEmpty()) {
            criteria.put("NODE_USERNAME_EQ", replication.node.username.eq(nodeUsername));
        }
        return this;
    }

    public ReplicationSearchCriteria likeNodeUsername(String nodeUsername) {
        if (nodeUsername != null && !nodeUsername.isEmpty()) {
            criteria.put("NODE_USERNAME_LIKE", replication.node.username.like("%"+nodeUsername+"%"));
        }

        return this;
    }

    public ReplicationSearchCriteria withBagName(String bagName) {
        if (bagName != null && !bagName.isEmpty()) {
            criteria.put("BAG_NAME_EQ", replication.bag.name.eq(bagName));
        }
        return this;
    }

    public ReplicationSearchCriteria likeBagName(String bagName) {
        if (bagName != null && !bagName.isEmpty()) {
            criteria.put("NODE_USERNAME_LIKE", replication.bag.name.like("%"+bagName+"%"));
        }

        return this;
    }

    public ReplicationSearchCriteria withStatus(ReplicationStatus status) {
        if (status != null) {
            criteria.put(Params.STATUS, replication.status.eq(status));
        }
        return this;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
