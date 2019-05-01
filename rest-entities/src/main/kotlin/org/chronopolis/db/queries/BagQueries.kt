package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.records.BagRecord
import org.chronopolis.rest.models.enums.BagStatus
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.time.ZonedDateTime
import java.util.stream.Stream

data class BagSummary(val size: Long, val count: Long, val status: String)
data class BagsOverview(val stuck: Int, val summaries: List<BagSummary>)

/**
 * Collection of functions used for querying the [org.chronopolis.db.generated.tables.Bag] table
 *
 * @since 3.2.0
 * @author shake
 */
object BagQueries {

    /**
     * Retrieve a list of all filenames which are related to a [BagRecord]
     * No attempt to be lazy, just fetch all in to memory
     *
     * @param context the [DSLContext] to query the database
     * @param bag the [BagRecord] to retrieve filenames for
     * @return a [List] of all filenames as [String]s
     * @since 3.2.0
     */
    fun filenamesInBag(context: DSLContext, bag: BagRecord): List<String> {
        val files = Tables.FILE
        return context.selectFrom(files)
                .where(files.DTYPE.eq("BAG")).and(files.BAG_ID.eq(bag.id))
                .fetch(files.FILENAME)
    }

    /**
     * Retrieve the number of [org.chronopolis.db.generated.tables.records.AceTokenRecord]s
     * registered to a [BagRecord]
     *
     * This isn't expected to overflow, but it's good to keep in mind that this returns an [Int]
     *
     * @param context the [DSLContext] to query the database
     * @param bag the [BagRecord] to retrieve the token count of
     * @return the count as an [Int]
     * @since 3.2.0
     */
    fun tokenCountForBag(context: DSLContext, bag: BagRecord): Int {
        val token = Tables.ACE_TOKEN
        return context.selectCount()
                .from(token)
                .where(token.BAG_ID.eq(bag.id))
                .fetchOne(0, Int::class.java)
    }

    /**
     * Retrieve [BagRecord]s which have an
     * [org.chronopolis.db.generated.tables.records.AceTokenRecord] for each
     * [org.chronopolis.db.generated.tables.records.FileRecord]
     *
     * @param context the [DSLContext] to query the database
     * @return a [Stream] of [BagRecord]s which can be processed
     * @since 3.2.0
     */
    fun bagsCompletedTokenization(context: DSLContext): Stream<BagRecord> {
        val bag = Tables.BAG
        val aceToken = Tables.ACE_TOKEN

        return context.selectFrom(bag)
                .where(bag.STATUS.eq(BagStatus.INITIALIZED))
                // we need to cast total_files to an int to compare it with the result of selectCount
                .and(bag.TOTAL_FILES.cast(Int::class.java)
                        .eq(context.selectCount()
                                .from(aceToken)
                                .where(aceToken.BAG_ID.eq(bag.ID))))
                .fetchStream()
    }

    /**
     * Retrieve [BagRecord]s which can be processed by a local tokenization task
     *
     * @param context the [DSLContext] to query the database
     * @param creator the creator to query on when retrieving bags
     * @return a [Stream] of [BagRecord]s
     * @since 3.2.0
     */
    fun localBagsForTokenization(context: DSLContext, creator: String): Stream<BagRecord> {
        val bag = Tables.BAG
        val file = Tables.FILE
        val staging = Tables.STAGING_STORAGE

        // todo: try to filter bags with all tokens?
        //       bag.total_files < selectCount().from(tokens)
        //       which makes me wonder if we should cache the number of registered files/tokens
        return context.select().from(bag)
                .innerJoin(staging)
                .on(bag.ID.eq(staging.BAG_ID)).and(staging.ACTIVE.isTrue)
                .innerJoin(file)
                .on(staging.FILE_ID.eq(file.ID)).and(file.DTYPE.eq("BAG"))
                .where(bag.STATUS.eq(BagStatus.INITIALIZED))
                .and(bag.CREATOR.eq(creator))
                .and(bag.TOTAL_FILES.cast(Int::class.java)
                        .eq(context.selectCount()
                                .from(file)
                                .where(file.BAG_ID.eq(bag.ID))))
                .fetchStreamInto(bag)

    }

    /**
     * Retrieve a list of [BagSummary]s grouped by their [BagStatus]
     *
     * @param context the [DSLContext] to query the database
     * @param states the [BagStatus] to query for
     * @return a [List] containing each [BagSummary]
     * @since 3.2.0
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
     * Retrieve an overview of the Bags stored in the database. This includes summaries for each
     * processing or preserved status and the number of stuck bags (updated at > 1 week and
     * processing). Additional processing (summarizing the processing bags) is done externally from
     * this function. This is solely for querying the database, unless we find a good way to do
     * aggregation as well (maybe with a view or something of the like).
     *
     * @param context the [DSLContext] to query the database
     * @return the [BagsOverview]
     * @since 3.2.0
     */
    fun bagsOverview(context:DSLContext): BagsOverview {
        val bag = Tables.BAG
        val processing = BagStatus.processingStates().map { it.toString() }
        val oneWeek = ZonedDateTime.now().minusWeeks(1).toLocalDateTime()

        val stuck = context.selectCount()
                .from(bag)
                .where(bag.STATUS.`in`(processing))
                .and(bag.UPDATED_AT.lt(oneWeek))
                .fetchOne(0, Int::class.java)

        val queryStates = BagStatus.processingStates().plus(BagStatus.preservedStates())
        val summaries = statsByGroup(context, queryStates)
        return BagsOverview(stuck, summaries)
    }
}