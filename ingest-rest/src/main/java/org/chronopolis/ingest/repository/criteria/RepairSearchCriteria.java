package org.chronopolis.ingest.repository.criteria;

import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.api.Params;
import org.chronopolis.rest.entities.repair.QRepair;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.RepairStatus;

import java.util.HashMap;
import java.util.List;
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

    public RepairSearchCriteria withStatuses(List<RepairStatus> statuses) {
        if (statuses != null) {
            criteria.put(Params.STATUS, repair.status.in(statuses));
        }
        return this;
    }

    public RepairSearchCriteria withValidated(String validated) {
        if (validated != null && !validated.isEmpty()) {
            criteria.put("VALIDATED", repair.validated.eq(Boolean.valueOf(validated)));
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

    public RepairSearchCriteria withFulfillingNode(String fulfillingNode) {
        if (fulfillingNode != null && !fulfillingNode.isEmpty()) {
            criteria.put("FULFILLING_NODE", repair.from.username.eq(fulfillingNode));
        }

        return this;
    }

    public RepairSearchCriteria withAuditStatuses(List<AuditStatus> auditStatus) {
        if (auditStatus != null && !auditStatus.isEmpty()) {
            criteria.put("AUDIT_STATUS", repair.audit.in(auditStatus));
        }
        return this;
    }
}
