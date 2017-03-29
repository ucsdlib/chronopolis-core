package org.chronopolis.ingest.repository.criteria;

import com.mysema.query.types.expr.BooleanExpression;

import java.util.Map;

/**
 *
 * Created by shake on 1/24/17.
 */
public interface SearchCriteria {
    Map<Object, BooleanExpression> getCriteria();
}
