package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.jooq.AggregateFunction
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.SelectSeekStep1
import org.jooq.Table
import org.jooq.impl.DSL.avg
import org.jooq.impl.DSL.coalesce
import org.jooq.impl.DSL.count
import org.jooq.impl.DSL.sum
import java.math.BigDecimal

data class DepositorSum(val sum: Long, val namespace: String)
data class DepositorCount(val count: Int, val namespace: String)
data class DepositorSummary(val avgSum: BigDecimal, val avgCount: BigDecimal, val total: Int)

/**
 * Functions for querying the [Depositor] table
 *
 * @since 3.2.0
 * @author shake
 */
object DepositorQueries {

    /**
     * Retrieve a [DepositorSummary] which tells basic information about Depositor holdings in the
     * database.
     *
     * @param ctx the [DSLContext] for querying the database
     * @return [DepositorSummary]
     * @since 3.2.0
     */
    fun depositorsSummary(ctx: DSLContext): DepositorSummary {
        val bag = Tables.BAG
        val depositorId = bag.DEPOSITOR_ID
        val sizeStmt = avg(bag.SIZE).`as`("size")
        val countStmt = count(bag.ID).`as`("count")

        // keep it in line with sum, count, total from DepositorSummary
        val nested: Table<Record3<BigDecimal, Int, Long>> =
                ctx.select(sizeStmt, countStmt, depositorId)
                        .from(bag)
                        .groupBy(depositorId)
                        .asTable("nested")

        // coalesce to ensure non-null result
        val sizeAvg = coalesce(avg(nested.field("size", BigDecimal::class.java)), BigDecimal(0))
        val countAvg = coalesce(avg(nested.field("count", Int::class.java)), BigDecimal(0))
        val averages = ctx.select(sizeAvg, countAvg)
                .from(nested)
                .fetchOne()

        val total = ctx.selectCount()
                .from(Tables.DEPOSITOR)
                .fetchOne(0, Int::class.java)

        return DepositorSummary(averages.value1(), averages.value2(), total)
    }

    /**
     * Get depositors ordered by sum of their ingested bags. Omits depositors with no data stored.
     *
     * @param ctx the [DSLContext] for querying the database
     * @param limit the number of depositors to retrieve
     * @return [List] containing the [DepositorSum] information
     * @since 3.2.0
     */
    fun topDepositorsBySum(ctx: DSLContext, limit: Int): List<DepositorSum> {
        val bag = Tables.BAG
        return aggregateQuery(ctx, sum(bag.SIZE))
                .limit(limit)
                .fetchInto(DepositorSum::class.java)
    }

    /**
     * Get depositors ordered by the number of their ingested bags. Omits depositors with no data
     * stored.
     *
     * @param ctx the [DSLContext] for querying the database
     * @param limit the number of depositors to retrieve
     * @return [List] containing the [DepositorCount] information
     * @since 3.2.0
     */
    fun topDepositorsByCount(ctx: DSLContext, limit: Int): List<DepositorCount> {
        val bag = Tables.BAG

        return aggregateQuery(ctx, count(bag))
                .limit(limit)
                .fetchInto(DepositorCount::class.java)
    }

    private fun <T> aggregateQuery(ctx: DSLContext,
                                   aggregate: AggregateFunction<T>):
            SelectSeekStep1<out Record2<out T, String>, out T> {
        val bag = Tables.BAG
        val depositor = Tables.DEPOSITOR
        return ctx.select(aggregate, depositor.NAMESPACE)
                .from(bag)
                .join(depositor).on(bag.DEPOSITOR_ID.eq(depositor.ID))
                .groupBy(depositor.NAMESPACE)
                .orderBy(aggregate.desc())
    }
}