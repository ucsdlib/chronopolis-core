package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.StorageRegion
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.math.BigDecimal


/**
 * Get the amount of space used by active [StagingStorage] for a [StorageRegion]
 *
 * @since 3.2.0
 * @author shake
 */
fun usedSpace(ctx: DSLContext, region: StorageRegion): BigDecimal {
    val staging = Tables.STAGING_STORAGE
    return ctx.select(DSL.sum(staging.SIZE))
            .from(staging)
            .where(staging.REGION_ID.eq(region.ID).and(staging.ACTIVE.isTrue))
            .fetchOne(0, BigDecimal::class.java)
}
