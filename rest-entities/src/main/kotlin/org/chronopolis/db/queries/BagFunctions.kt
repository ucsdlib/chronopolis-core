package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.Bag
import org.chronopolis.db.generated.tables.records.BagRecord
import org.jooq.DSLContext

/**
 * Retrieve a list of all filenames which live in a [Bag]
 * No attempt to be lazy, just fetch all in to memory
 *
 * @since 3.2.0
 * @author shake
 * @return a [MutableList] of all filenames as [String]s
 */
fun filenamesInBag(context: DSLContext, bag: BagRecord): MutableList<String> {
    val files = Tables.FILE
    return context.selectFrom(files)
            .where(files.DTYPE.eq("BAG").and(files.BAG_ID.eq(bag.id)))
            .fetch(files.FILENAME)
}

/**
 * Retrieve the number of [AceToken]s registered for a [Bag]
 *
 * This isn't expected to overflow, but it's good to keep in mind that this returns an [Int]
 *
 * @since 3.2.0
 * @author shake
 * @return the count as an [Int]
 */
fun tokenCountForBag(context: DSLContext, bag: BagRecord): Int {
    val token = Tables.ACE_TOKEN
    return context.selectCount()
            .from(token)
            .where(token.BAG_ID.eq(bag.id))
            .fetchOne(0, Int::class.java)
}
