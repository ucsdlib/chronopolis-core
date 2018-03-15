package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.QReplication;
import org.chronopolis.rest.models.ReplicationStatus;

import java.util.List;

/**
 * Model for binding data when filtering on replications
 *
 * Created by shake on 6/15/17.
 */
public class ReplicationFilter extends Paged {

    private final BooleanBuilder builder = new BooleanBuilder();
    private final QReplication replication = QReplication.replication;

    private String node;
    private String bag;
    private List<ReplicationStatus> status;

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getNode() {
        return node;
    }

    public ReplicationFilter setNode(String node) {
        if (node != null && !node.isEmpty()) {
            this.node = node;
            parameters.put("node", node);
            builder.and(replication.node.username.eq(node));
        }
        return this;
    }

    public String getBag() {
        return bag;
    }

    public ReplicationFilter setBag(String bag) {
        if (bag != null && !bag.isEmpty()) {
            this.bag = bag;
            parameters.put("bag", bag);
            builder.and(replication.bag.name.eq(bag));
        }
        return this;
    }

    public List<ReplicationStatus> getStatus() {
        return status;
    }

    public ReplicationFilter setStatus(List<ReplicationStatus> status) {
        if (status != null && !status.isEmpty()) {
            this.status = status;
            status.forEach(replicationStatus -> parameters.put("status", replicationStatus.name()));
            builder.and(replication.status.in(status));
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

        switch (getOrderBy()) {
            case "bag":
                orderSpecifier = new OrderSpecifier<>(dir, replication.bag.id);
                break;
            case "createdAt":
                orderSpecifier = new OrderSpecifier<>(dir, replication.createdAt);
                break;
            case "updatedAt":
                orderSpecifier = new OrderSpecifier<>(dir, replication.updatedAt);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(dir, replication.id);
                break;
        }

        return orderSpecifier;
    }
}
