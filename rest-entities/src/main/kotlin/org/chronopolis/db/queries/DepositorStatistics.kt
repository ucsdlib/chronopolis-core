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


/**
 * Basic summary object for [Depositor]s of the form (average_sum, average_count, total)
 *
 * @since 3.2.0
 * @author shake
 */
fun depositorsSummary(ctx: DSLContext): DepositorSummary {
    val total = ctx.selectCount()
            .from(Tables.DEPOSITOR)
            .fetchOne(0, Int::class.java)

    val averages = depositorAverages(ctx)

    return DepositorSummary(averages.value1(), averages.value2(), total)
}

// merge with depositorSummary?
private fun depositorAverages(ctx: DSLContext): Record2<BigDecimal, BigDecimal> {
    val bag = Tables.BAG
    val depositorId = bag.DEPOSITOR_ID
    val sizeStmt = DSL.avg(bag.SIZE).`as`("size")
    val countStmt = DSL.count(bag.ID).`as`("count")

    // keep it in line with sum, count, total from DepositorSummary
    val nested: Table<Record3<BigDecimal, Int, Long>> = ctx.select(sizeStmt, countStmt, depositorId)
            .groupBy(depositorId)
            .asTable("nested")

    val sizeAvg = DSL.avg(nested.field("size", BigDecimal::class.java))
    val countAvg = DSL.avg(nested.field("count", Int::class.java))
    return ctx.select(sizeAvg, countAvg)
            .from(nested)
            .fetchOne()
}

/**
 * Top depositors by sum
 *
 * todo: allow flexible limit?
 *
 * @since 3.2.0
 * @author shake
 */
fun topDepositorsBySum(ctx: DSLContext): MutableList<DepositorSum> {
    val bag = Tables.BAG

    // this would fail on runtime.... uhm...
    return aggregateQuery(ctx, DSL.sum(bag.SIZE))
            .limit(5)
            .fetchInto(DepositorSum::class.java)
}

/**
 * Top depositors by count
 *
 * todo: allow flexible limit?
 *
 * @since 3.2.0
 * @author shake
 */
fun topDepositorsByCount(ctx: DSLContext): MutableList<DepositorCount> {
    val bag = Tables.BAG

    return aggregateQuery(ctx, DSL.count(bag))
            .limit(5)
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


data class DepositorSum(val sum: Long, val namespace: String)
data class DepositorCount(val count: Int, val namespace: String)
data class DepositorSummary(val avgSum: BigDecimal, val avgCount: BigDecimal, val total: Int)
