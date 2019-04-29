package org.chronopolis.db.binding

import org.chronopolis.db.Limit
import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.models.enums.BagStatus
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

/**
 * Moo
 *
 * I ain't gotten frozen by the way you walk
 *
 */
class BagPageable() : Pageable() {

    private val bag = Tables.BAG

    fun setName(name: String) {
        conditions.add(bag.NAME.eq(name))
    }

    fun setCreator(creator: String) {
        conditions.add(bag.CREATOR.eq(creator))
    }

    fun setDepositor(depositor: String) {
        // how does this work? needs to be a join....
        // which means when we query for bags we need to make sure all relevant tables are joined
        conditions.add(bag.depositor().NAMESPACE.eq(depositor))
    }

    fun setStatus(status: List<BagStatus>) {
        conditions.add(bag.STATUS.`in`(status.map { status::toString }))
    }

}