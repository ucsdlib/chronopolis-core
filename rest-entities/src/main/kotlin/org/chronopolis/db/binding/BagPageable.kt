package org.chronopolis.db.binding

import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.models.enums.BagStatus

/**
 * Query parameters for [org.chronopolis.db.generated.tables.Bag]
 *
 * @since 3.2.0
 * @author shake
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
        // note: this will add a join on the depositor table
        conditions.add(bag.depositor().NAMESPACE.eq(depositor))
    }

    fun setStatus(status: List<BagStatus>) {
        conditions.add(bag.STATUS.`in`(status.map { status::toString }))
    }

}