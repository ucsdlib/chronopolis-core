package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.models.enums.ReplicationStatus
import org.jooq.DSLContext
import java.time.ZonedDateTime

/**
 * Retrieve a [ReplicationSummary] for displaying information about the number of replications and
 * stuck [Replication] requests
 *
 * @param ctx the [DSLContext] used for querying the database
 * @return the [ReplicationSummary]
 * @since 3.2.0
 * @author shake
 */
fun replicationStats(ctx: DSLContext): ReplicationSummary {
    val now = ZonedDateTime.now()
    val oneWeek = ZonedDateTime.now().minusWeeks(1).toLocalDateTime();
    val twoWeeks = ZonedDateTime.now().minusWeeks(2).toLocalDateTime();
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
            .where(query.and(replication.UPDATED_AT.lt(oneWeek)))
            .fetchOne(0, Int::class.java)
    val stuckTwo = ctx.selectCount()
            .from(replication)
            .where(query.and(replication.UPDATED_AT.lt(twoWeeks)))
            .fetchOne(0, Int::class.java)

    return ReplicationSummary(total, stuckOne, stuckTwo)
}

data class ReplicationSummary(val total: Int, val stuckOne: Int, val stuckTwo: Int)

