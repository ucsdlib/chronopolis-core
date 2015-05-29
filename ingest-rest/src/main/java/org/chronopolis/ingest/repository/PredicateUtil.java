package org.chronopolis.ingest.repository;

import com.mysema.query.types.expr.BooleanExpression;

/**
 * Class with helper functions common to our needs for querydsl
 *
 * Created by shake on 5/21/15.
 */
public class PredicateUtil {

    public static BooleanExpression setExpression(BooleanExpression predicate, BooleanExpression other) {
        // If the predicate is null, use the other expression
        if (predicate == null) {
            return other;
        }

        // Else return the combination of the two
        return predicate.and(other);
    }

}
