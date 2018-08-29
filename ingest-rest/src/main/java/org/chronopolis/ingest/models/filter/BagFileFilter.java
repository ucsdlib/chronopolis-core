package org.chronopolis.ingest.models.filter;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.chronopolis.ingest.models.Paged;
import org.chronopolis.rest.entities.BagFile;
import org.chronopolis.rest.entities.QBagFile;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Query Parameters for a {@link BagFile}
 * <p>
 * Ok so this is sufficiently different than the other Paged impls and we need to see if the data
 * binding will still work. It's more similar to the old SearchCriteria; we might want to store
 * the parameters as well but that's for later.
 *
 * @author shake
 */
public class BagFileFilter extends Paged {

    private final QBagFile qBagFile = QBagFile.bagFile;
    private final BooleanBuilder builder = new BooleanBuilder();

    private Map<String, BooleanExpression> expressions = new HashMap<>();

    @Override
    public BooleanBuilder getQuery() {
        expressions.values().forEach(builder::and);
        return builder;
    }

    @Override
    public OrderSpecifier getOrderSpecifier() {
        Order direction = getDirection();
        OrderSpecifier orderSpecifier;

        switch (getOrderBy()) {
            case "bag":
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.bag.id);
                break;
            case "size":
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.size);
                break;
            case "filename":
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.filename);
                break;
            case "createdAt":
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.createdAt);
                break;
            case "updatedAt":
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.updatedAt);
                break;
            default:
                orderSpecifier = new OrderSpecifier<>(direction, qBagFile.id);
        }

        return orderSpecifier;
    }


    public BagFileFilter setFixity(String fixity) {
        if (fixity != null && !fixity.isEmpty()) {
            expressions.put("fixity", qBagFile.fixities.any().value.eq(fixity));
        }
        return this;
    }


    public BagFileFilter setAlgorithm(String algorithm) {
        if (algorithm != null && !algorithm.isEmpty()) {

            expressions.put("algorithm", qBagFile.fixities.any().algorithm.eq(algorithm));
        }
        return this;
    }

    public BagFileFilter setBag(Long bag) {
        expressions.put("bag", qBagFile.bag.id.eq(bag));
        return this;
    }

    public BagFileFilter setSizeLess(Long sizeLess) {
        expressions.put("sizeLess", qBagFile.size.lt(sizeLess));
        return this;
    }

    public BagFileFilter setSizeGreater(Long sizeGreater) {
        expressions.put("sizeGreater", qBagFile.size.gt(sizeGreater));
        return this;
    }

    public BagFileFilter setCreatedBefore(ZonedDateTime createdBefore) {
        expressions.put("createdBefore", qBagFile.createdAt.before(createdBefore));
        return this;
    }

    public BagFileFilter setCreatedAfter(ZonedDateTime createdAfter) {
        expressions.put("createdAfter", qBagFile.createdAt.after(createdAfter));
        return this;
    }

    public BagFileFilter setUpdatedBefore(ZonedDateTime updatedBefore) {
        expressions.put("updatedBefore", qBagFile.updatedAt.before(updatedBefore));
        return this;
    }

    public BagFileFilter setUpdatedAfter(ZonedDateTime updatedAfter) {
        expressions.put("updatedAfter", qBagFile.updatedAt.after(updatedAfter));
        return this;
    }

}
