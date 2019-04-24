package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.models.enums.ReplicationStatus
import org.jooq.DSLContext
import java.time.LocalDateTime

/**
 * Replication table queries
 *
 * @since 3.2.0
 * @author shake
 */
object ReplicationQueries {

    fun replicationSummary(ctx: DSLContext): ReplicationSummary {
        val oneWeek = LocalDateTime.now().minusWeeks(1)
        val twoWeeks = LocalDateTime.now().minusWeeks(2)
        val replication = Tables.REPLICATION
        val states = ReplicationStatus.active().map { it.toString() }
        val query = replication.STATUS.`in`(states)

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

    data class ReplicationSummary(val total: Int, val stuckOneWeek: Int, val stuckTwoWeeks: Int)
}