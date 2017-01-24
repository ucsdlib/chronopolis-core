package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QFulfillment;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.FulfillmentType;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by shake on 1/24/17.
 */
public class FulfillmentSearchCriteria implements SearchCriteria {

    private final QFulfillment qFulfillment;
    private Map<Object, BooleanExpression> criteria;

    public FulfillmentSearchCriteria() {
        qFulfillment = QFulfillment.fulfillment;
        criteria = new HashMap<>();
    }

    public FulfillmentSearchCriteria withFrom(String from) {
        if (from != null && !from.isEmpty()) {
            criteria.put(Params.NODE, qFulfillment.from.eq(from));
        }

        return this;
    }

    public FulfillmentSearchCriteria withStatus(FulfillmentStatus status) {
        if (status != null) {
            criteria.put(Params.STATUS, qFulfillment.status.eq(status));
        }

        return this;
    }

    public FulfillmentSearchCriteria withType(FulfillmentType type) {
        if (type != null) {
            criteria.put(Params.TYPE, qFulfillment.type.eq(type));
        }

        return this;
    }

    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }
}
