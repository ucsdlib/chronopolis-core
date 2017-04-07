package org.chronopolis.ingest.repository.criteria;

import com.mysema.query.types.expr.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.QRepair;
import org.chronopolis.rest.models.repair.FulfillmentStatus;
import org.chronopolis.rest.models.repair.RepairStatus;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * Created by shake on 1/24/17.
 */
public class RepairSearchCriteria implements SearchCriteria {
    private final QRepair repair;

    private Map<Object, BooleanExpression> criteria;

    public RepairSearchCriteria() {
        repair = QRepair.repair;
        criteria = new HashMap<>();
    }

    public RepairSearchCriteria withId(Long id) {
        if (id != null) {
            criteria.put("id", repair.id.eq(id));
        }

        return this;
    }

    public RepairSearchCriteria withTo(String to) {
        if (to != null && !to.isEmpty()) {
            criteria.put(Params.TO, repair.to.username.eq(to));
        }

        return this;
    }

    public RepairSearchCriteria withStatus(RepairStatus status) {
        if (status != null) {
            criteria.put(Params.STATUS, repair.status.eq(status));
        }
        return this;
    }

    public RepairSearchCriteria withFulfillmentStatus(FulfillmentStatus status) {
        if (status != null) {
            criteria.put("FULFILLMENT_STATUS", repair.fulfillment.status.eq(status));
        }
        return this;
    }

    public RepairSearchCriteria withFulfillmentValidated(String validated) {
        if (validated != null && !validated.isEmpty()) {
            criteria.put("FULFILLMENT_VALIDATED", repair.fulfillment.validated.eq(Boolean.valueOf(validated)));
        }
        return this;
    }

    public RepairSearchCriteria withRequester(String requester) {
        if (requester != null && !requester.isEmpty()) {
            criteria.put(Params.REQUESTER, repair.requester.eq(requester));
        }

        return this;
    }


    @Override
    public Map<Object, BooleanExpression> getCriteria() {
        return criteria;
    }

    public RepairSearchCriteria withCleaned(String cleaned) {
        if (cleaned != null && !cleaned.isEmpty()) {
            criteria.put("REPLACED", repair.cleaned.eq(Boolean.valueOf(cleaned)));
        }
        return this;
    }

    public RepairSearchCriteria withReplaced(String replaced) {
        if (replaced != null && !replaced.isEmpty()) {
            criteria.put("REPLACED", repair.replaced.eq(Boolean.valueOf(replaced)));
        }
        return this;
    }
}
