package org.chronopolis.entities

import org.chronopolis.entities.tables.Bag
import org.jooq.DSLContext

fun filenamesInBag(context: DSLContext, bag: Bag): MutableList<String> {
    val files = Tables.FILE
    return context.selectFrom(files)
            .where(files.DTYPE.eq("BAG").and(files.BAG_ID.eq(bag.ID)))
            .fetch(files.FILENAME)
}

fun tokenCountForBag(context: DSLContext, bag: Bag): Int {
    val token = Tables.ACE_TOKEN
    return context.selectCount()
            .from(token)
            .where(token.BAG_ID.eq(bag.ID))
            .fetchOne(0, Int::class.java)
}
