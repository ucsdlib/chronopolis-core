package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.repair.QRepair;
import org.chronopolis.rest.models.enums.AuditStatus;
import org.chronopolis.rest.models.enums.RepairStatus;

import java.util.List;

/**
 * Data binding for filtering on Repairs
 *
 * Created by shake on 6/15/17.
 */
public class RepairFilter extends Paged {

    private final QRepair repair = QRepair.repair;
    private final BooleanBuilder builder = new BooleanBuilder();

    private String node;
    private String fulfillingNode;
    private List<RepairStatus> status;
    private List<AuditStatus> auditStatus;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getNode() {
        return node;
    }

    public RepairFilter setNode(String node) {
        if (node != null && !node.isEmpty()) {
            this.node = node;
            parameters.put("node", node);
            builder.and(repair.to.username.eq(node));
        }
        return this;
    }

    public String getFulfillingNode() {
        return fulfillingNode;
    }

    public RepairFilter setFulfillingNode(String fulfillingNode) {
        if (fulfillingNode != null && !fulfillingNode.isEmpty()) {
            this.fulfillingNode = fulfillingNode;
            parameters.put("fulfillingNode", fulfillingNode);
            builder.and(repair.from.username.eq(fulfillingNode));
        }
        return this;
    }

    public List<RepairStatus> getStatus() {
        return status;
    }

    public RepairFilter setStatus(List<RepairStatus> status) {
        if (status != null && !status.isEmpty()) {
            this.status = status;
            status.forEach(repairStatus -> parameters.put("status", repairStatus.name()));
            builder.and(repair.status.in(status));
        }
        return this;
    }

    public List<AuditStatus> getAuditStatus() {
        return auditStatus;
    }

    public RepairFilter setAuditStatus(List<AuditStatus> auditStatus) {
        if (auditStatus != null && !auditStatus.isEmpty()) {
            this.auditStatus = auditStatus;
            auditStatus.forEach(status -> parameters.put("auditStatus", status.name()));
            builder.and(repair.audit.in(auditStatus));
        }
        return this;
    }

    public Multimap<String, String> getParameters() {
        parameters.putAll(super.getParameters());
        return Multimaps.filterValues(parameters, (value) -> (value != null && !value.isEmpty()));
    }

    @Override
    public BooleanBuilder getQuery() {
        return builder;
    }

    @Override
    public OrderSpecifier getOrderSpecifier() {
        Order dir = getDirection();
        OrderSpecifier orderSpecifier;

        //noinspection Duplicates
        switch (getOrderBy()) {
            case "createdAt":
                orderSpecifier = new OrderSpecifier<>(dir, repair.createdAt);
                break;
            case "updatedAt":
                orderSpecifier = new OrderSpecifier<>(dir, repair.updatedAt);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(dir, repair.id);
                break;
        }

        return orderSpecifier;
    }
}
