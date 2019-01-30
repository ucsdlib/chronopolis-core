package org.chronopolis.ingest.models.filter;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.depositor.QDepositor;

/**
 * Query Parameters for the Depositors
 *
 * @author shake
 */
public class DepositorFilter extends Paged {

    private final QDepositor depositor = QDepositor.depositor;
    private final BooleanBuilder builder = new BooleanBuilder();

    private String namespace;
    private final Multimap<String, String> parameters = LinkedListMultimap.create();

    public String getNamespace() {
        return namespace;
    }

    public DepositorFilter setNamespace(String namespace) {
        if (namespace != null) {
            this.namespace = namespace;
            parameters.put("namespace", namespace);
            builder.and(depositor.namespace.eq(namespace));
        }

        return this;
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
                orderSpecifier = new OrderSpecifier<>(dir, depositor.createdAt);
                break;
            case "updatedAt":
                orderSpecifier = new OrderSpecifier<>(dir, depositor.updatedAt);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(dir, depositor.id);
                break;
        }

        return orderSpecifier;
    }

    @Override
    public Multimap<String, String> getParameters() {
        parameters.putAll(super.getParameters());
        return Multimaps.filterValues(parameters, (value) -> (value != null && !value.isEmpty()));
    }

}
