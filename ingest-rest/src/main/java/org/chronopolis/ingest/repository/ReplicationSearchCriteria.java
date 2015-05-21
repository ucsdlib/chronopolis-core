package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.rest.models.QReplication;
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
    private Long id;
    private Long bagId;
    private String nodeUsername;
    private ReplicationStatus status;

    public ReplicationSearchCriteria() {
        replication = QReplication.replication;
        criteria = new HashMap<>();
    }

    public ReplicationSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put(id, replication.id.eq(id));
            this.id = id;
        }
        return this;
    }

    public ReplicationSearchCriteria withBagId(Long bagId) {
        if (bagId != null) {
            criteria.put(bagId, replication.bag.ID.eq(bagId));
            this.bagId = bagId;
        }
        return this;
    }

    public ReplicationSearchCriteria withNodeUsername(String nodeUsername) {
        if (nodeUsername != null) {
            criteria.put(nodeUsername, replication.node.username.eq(nodeUsername));
            this.nodeUsername = nodeUsername;
        }
        return this;
    }

    public ReplicationSearchCriteria withStatus(ReplicationStatus status) {
        if (status != null) {
            criteria.put(status, replication.status.eq(status));
            this.status = status;
        }
        return this;
    }

    public Long getId() {
        return id;
    }

    public Long getBagId() {
        return bagId;
    }

    public String getNodeUsername() {
        return nodeUsername;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
