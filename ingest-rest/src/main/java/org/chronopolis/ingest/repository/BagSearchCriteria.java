package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QBag;
import org.chronopolis.rest.models.BagStatus;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Search criteria that map to the query parameters one may pass in when getting bags
 *
 * Created by shake on 5/20/15.
 */
public class BagSearchCriteria {
    private final QBag bag;

    // TODO: We could do a multimap in order to get OR relations
    private Map<Object, BooleanExpression> criteria;

    public BagSearchCriteria() {
        this.bag = QBag.bag;
        this.criteria = new HashMap<>();
    }

    public BagSearchCriteria withName(String name) {
        if (name != null && !name.isEmpty()) {
            criteria.put(Params.NAME, bag.name.eq(name));
        }
        return this;
    }

    public BagSearchCriteria likeName(String name) {
        if (name != null && !name.isEmpty()) {
            criteria.put(Params.NAME, bag.name.like("%" + name + "%"));
        }
        return this;
    }

    public BagSearchCriteria withDepositor(String depositor) {
        if (depositor != null && !depositor.isEmpty()) {
            criteria.put(Params.DEPOSITOR, bag.depositor.eq(depositor));
        }
        return this;
    }

    public BagSearchCriteria likeDepositor(String depositor) {
        if (depositor != null && !depositor.isEmpty()) {
            criteria.put(Params.DEPOSITOR, bag.depositor.like("%" + depositor + "%"));
        }
        return this;
    }

    public BagSearchCriteria withStatus(BagStatus status) {
        if (status != null) {
            criteria.put(Params.STATUS, bag.status.eq(status));
        }
        return this;
    }

    public BagSearchCriteria updatedAfter(ZonedDateTime datetime) {
        if (datetime != null) {
            criteria.put("UPDATED_AFTER", bag.updatedAt.after(datetime));
        }
        return this;
    }

    public BagSearchCriteria updatedBefore(ZonedDateTime datetime) {
        if (datetime != null) {
            criteria.put("UPDATED_BEFORE", bag.updatedAt.before(datetime));
        }
        return this;
    }

    public BagSearchCriteria createdAfter(ZonedDateTime datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", bag.createdAt.after(datetime));
        }
        return this;
    }

    public BagSearchCriteria createdBefore(ZonedDateTime datetime) {
        if (datetime != null) {
            criteria.put("CREATED_AFTER", bag.createdAt.before(datetime));
        }
        return this;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
