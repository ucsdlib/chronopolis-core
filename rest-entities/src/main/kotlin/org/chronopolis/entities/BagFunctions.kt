package org.chronopolis.entities

import org.chronopolis.entities.tables.Bag
import org.jooq.DSLContext

/**
 * Retrieve a list of all filenames which live in a [Bag]
 * No attempt to be lazy, just fetch all in to memory
 *
 * @since 3.2.0
 * @author shake
 * @return a [MutableList] of all filenames as [String]s
 */
fun filenamesInBag(context: DSLContext, bag: Bag): MutableList<String> {
    val files = Tables.FILE
    return context.selectFrom(files)
            .where(files.DTYPE.eq("BAG").and(files.BAG_ID.eq(bag.ID)))
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
fun tokenCountForBag(context: DSLContext, bag: Bag): Int {
    val token = Tables.ACE_TOKEN
    return context.selectCount()
            .from(token)
            .where(token.BAG_ID.eq(bag.ID))
            .fetchOne(0, Int::class.java)
}
