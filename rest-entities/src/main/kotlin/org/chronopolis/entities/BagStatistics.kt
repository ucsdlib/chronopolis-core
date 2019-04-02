package org.chronopolis.entities

import org.chronopolis.rest.models.enums.BagStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL

fun statsByGroup(context: DSLContext) : List<BagSummary> {
    val preserved = BagStatus.PRESERVED.toString()
    val replicating = BagStatus.REPLICATING.toString()
    val bag = Tables.BAG

    return context.select(DSL.sum(bag.SIZE), DSL.count(bag), bag.STATUS).from(bag)
                    .where(bag.STATUS.`in`(preserved, replicating))
                    .groupBy(bag.STATUS)
                    .fetchInto(BagSummary::class.java)
}

data class BagSummary(val size: Long, val count: Long, val status: String);