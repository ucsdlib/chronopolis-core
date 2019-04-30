package org.chronopolis.db.binding

import org.chronopolis.db.Limit
import org.jooq.Condition
import org.jooq.OrderField

/**
 * Holds common paging attributes
 *
 * @since 3.2.0
 * @author shake
 */
abstract class Pageable {

    private var page: Int = 0
    private var pageSize: Int = 25

    internal val order: MutableSet<OrderField<*>> = mutableSetOf()
    internal val conditions: MutableSet<Condition> = mutableSetOf()

    fun getLimit(): Limit {
        return Limit(pageSize, page * pageSize)
    }

    fun getOrder(): Collection<OrderField<*>> {
        return order
    }

    fun getConditions(): Collection<Condition> {
        return conditions
    }

}

