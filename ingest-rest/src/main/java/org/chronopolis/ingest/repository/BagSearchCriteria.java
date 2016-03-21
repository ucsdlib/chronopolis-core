package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.models.BagStatus;
import org.chronopolis.rest.entities.QBag;

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
    private String name;
    private String depositor;
    private BagStatus status;

    public BagSearchCriteria() {
        this.bag = QBag.bag;
        this.criteria = new HashMap<>();
    }

    public BagSearchCriteria withName(String name) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
            criteria.put(Params.NAME, bag.name.eq(name));
        }
        return this;
    }

    public BagSearchCriteria withDepositor(String depositor) {
        if (depositor != null && !depositor.isEmpty()) {
            this.depositor = depositor;
            criteria.put(Params.DEPOSITOR, bag.depositor.eq(depositor));
        }
        return this;
    }

    public BagSearchCriteria withStatus(BagStatus status) {
        if (status != null) {
            this.status = status;
            criteria.put(Params.STATUS, bag.status.eq(status));
        }
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDepositor() {
        return depositor;
    }

    public BagStatus getStatus() {
        return status;
    }

    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
