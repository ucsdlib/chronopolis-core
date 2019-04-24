package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.records.BagRecord
import org.chronopolis.rest.models.enums.BagStatus
import org.jooq.DSLContext
import java.util.stream.Stream

/**
 * Collection of functions used for querying the [Bag] table
 *
 * @since 3.2.0
 * @author shake
 */
object BagQueries {

    /**
     * Retrieve a list of all filenames which live in a [Bag]
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
     * Retrieve the number of [AceToken]s registered for a [Bag]
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
     * Retrieve [BagRecord]s which have an [AceToken] for each [File]
     *
     * @param context the [DSLContext] to query the database
     * @return a [Stream] of [BagRecord]s which can be processed
     * @since 3.2.0
     */
    fun bagsCompletedTokenization(context: DSLContext): Stream<BagRecord> {
        val bag = Tables.BAG
        val aceToken = Tables.ACE_TOKEN

        return context.selectFrom(bag)
                .where(bag.STATUS.eq(BagStatus.INITIALIZED.toString()))
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
                .where(bag.STATUS.eq(BagStatus.INITIALIZED.toString()))
                .and(bag.CREATOR.eq(creator))
                .and(bag.TOTAL_FILES.cast(Int::class.java)
                        .eq(context.selectCount()
                                .from(file)
                                .where(file.BAG_ID.eq(bag.ID))))
                .fetchStreamInto(bag)

    }
}