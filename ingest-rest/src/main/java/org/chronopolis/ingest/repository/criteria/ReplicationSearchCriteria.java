package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.models.enums.ReplicationStatus;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Search criteria for Replication transfers
 *
 * TODO: If we have the map, do we need the fields?
 *
 * Created by shake on 5/21/15.
 */
public class ReplicationSearchCriteria implements SearchCriteria {
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

    public ReplicationSearchCriteria nodeUsernameLike(String nodeUsername) {
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

    public ReplicationSearchCriteria bagNameLike(String bagName) {
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

    public ReplicationSearchCriteria withStatuses(Collection<ReplicationStatus> statuses) {
        if (statuses != null) {
            criteria.put(Params.STATUS, replication.status.in(statuses));
        }
        return this;
    }

    // TODO: We could have an UpdatedEntitySearchCriteria which serves as a base and holds these methods
    //       for our Bag/ReplicationSearchCriteria

    public ReplicationSearchCriteria updatedAfter(String datetime) {
        if (datetime != null) {
            ZonedDateTime.parse(datetime);
            criteria.put("UPDATED_AFTER", replication.updatedAt.after(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public ReplicationSearchCriteria updatedBefore(String datetime) {
        if (datetime != null) {
            criteria.put("UPDATED_BEFORE", replication.updatedAt.before(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public ReplicationSearchCriteria createdAfter(String datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", replication.createdAt.after(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public ReplicationSearchCriteria createdBefore(String datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", replication.createdAt.before(ZonedDateTime.parse(datetime)));
        }
        return this;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
