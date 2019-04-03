package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.rest.models.enums.BagStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.ZonedDateTime

/**
 * Retrieve a list of [BagSummary]s grouped by their [BagStatus]
 *
 * @param context the [DSLContext] used for querying the database
 * @param states the [BagStatus] states to query for
 * @return a [List] containing each [BagSummary]
 * @since 3.2.0
 * @author shake
 */
fun statsByGroup(context: DSLContext, states: Collection<BagStatus>): List<BagSummary> {
    val bag = Tables.BAG
    val queryStates = states.map { it.toString() }

    return context.select(DSL.sum(bag.SIZE), DSL.count(bag), bag.STATUS).from(bag)
            .where(bag.STATUS.`in`(queryStates))
            .groupBy(bag.STATUS)
            .fetchInto(BagSummary::class.java)
}

/**
 * Retrieve an overview of the Bags stored in the Database. This includes summaries for each
 * processing or preserved status and the number of stuck bags (updated at > 1 week and processing).
 * Additional processing (summarizing the processing bags) is done externally from this function.
 * This is solely for querying the database, unless we find a good way to do aggregation as well, as
 * it might be useful to embed some of this in the [BagsOverview] class (point to the preserved
 * summary, aggregate summary, etc).
 *
 * @param context the [DSLContext] used for querying the database
 * @return [BagsOverview] containing the number of stuck bags and list of summaries
 * @since 3.2.0
 * @author shake
 */
fun overview(context: DSLContext): BagsOverview {
    val bag = Tables.BAG
    val processing = BagStatus.processingStates().map { it.toString() }
    val oneWeek = ZonedDateTime.now().minusWeeks(1).toLocalDateTime();

    val stuck = context.selectCount()
            .from(bag)
            .where(bag.STATUS.`in`(processing)
                    .and(bag.UPDATED_AT.lt(oneWeek)))
            .fetchOne(0, Int::class.java)

    val queryStates = BagStatus.processingStates().plus(BagStatus.preservedStates())
    val summaries = statsByGroup(context, queryStates)

    return BagsOverview(stuck, summaries)
}

data class BagSummary(val size: Long, val count: Long, val status: String)
data class BagsOverview(val stuck: Int, val summaries: List<BagSummary>)