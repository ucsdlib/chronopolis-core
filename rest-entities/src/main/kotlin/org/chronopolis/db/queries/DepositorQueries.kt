package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.jooq.AggregateFunction
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Record3
import org.jooq.SelectSeekStep1
import org.jooq.Table
import org.jooq.impl.DSL
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

    fun depositorsSummary(ctx: DSLContext): DepositorSummary {
        val bag = Tables.BAG
        val depositorId = bag.DEPOSITOR_ID
        val sizeStmt = DSL.avg(bag.SIZE).`as`("size")
        val countStmt = DSL.count(bag.ID).`as`("count")

        // keep it in line with sum, count, total from DepositorSummary
        val nested: Table<Record3<BigDecimal, Int, Long>> =
                ctx.select(sizeStmt, countStmt, depositorId)
                        .groupBy(depositorId)
                        .asTable("nested")

        val sizeAvg = DSL.avg(nested.field("size", BigDecimal::class.java))
        val countAvg = DSL.avg(nested.field("count", Int::class.java))
        val averages = ctx.select(sizeAvg, countAvg)
                .from(nested)
                .fetchOne()

        val total = ctx.selectCount()
                .from(Tables.DEPOSITOR)
                .fetchOne(0, Int::class.java)

        return DepositorSummary(averages.value1(), averages.value2(), total)
    }

    /**
     * Get depositors ordered by sum of their ingested bags
     *
     * @param ctx
     * @param limit the number of depositors to retrieve
     * @return [List] containing the [DepositorSum] information
     * @since 3.2.0
     */
    fun topDepositorsBySum(ctx: DSLContext, limit: Int): List<DepositorSum> {
        val bag = Tables.BAG
        return aggregateQuery(ctx, DSL.sum(bag.SIZE))
                .limit(limit)
                .fetchInto(DepositorSum::class.java)
    }

    /**
     * Get depositors ordered by the number of their ingested bags
     *
     * @param limit the number of depositors to retrieve
     * @return [List] containing the [DepositorCount] information
     * @since 3.2.0
     */
    fun topDepositorsByCount(ctx: DSLContext, limit: Int): List<DepositorCount> {
        val bag = Tables.BAG

        return aggregateQuery(ctx, DSL.count(bag))
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
                .orderBy(aggregate)
    }
}