package org.chronopolis.entities

import org.chronopolis.rest.models.enums.ReplicationStatus
import org.jooq.DSLContext
import java.sql.Timestamp
import java.time.ZonedDateTime

fun replicationStats(ctx: DSLContext): ReplicationSummary {
    val now = ZonedDateTime.now()
    val replication = Tables.REPLICATION
    val states = ReplicationStatus.active().map { it.toString() }
    val query = replication.STATUS.`in`(states)

    // todo: might be nice to execute this as a single query instead of 3
    val total = ctx.selectCount()
            .from(replication)
            .where(query)
            .fetchOne(0, Int::class.java)
    val stuckOne = ctx.selectCount()
            .from(replication)
            .where(query.and(replication.UPDATED_AT.lt(now.minusWeeks(1).timestamp())))
            .fetchOne(0, Int::class.java)
    val stuckTwo = ctx.selectCount()
            .from(replication)
            .where(query.and(replication.UPDATED_AT.lt(now.minusWeeks(2).timestamp())))
            .fetchOne(0, Int::class.java)

    return ReplicationSummary(total, stuckOne, stuckTwo)
}

internal fun ZonedDateTime.timestamp(): Timestamp {
    return Timestamp.from(this.toInstant())
}

data class ReplicationSummary(val total: Int, val stuckOne: Int, val stuckTwo: Int)

