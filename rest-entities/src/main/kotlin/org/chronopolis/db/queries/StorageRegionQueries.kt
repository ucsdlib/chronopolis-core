package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.records.StorageRegionRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.math.BigDecimal

/**
 * You know the drill
 *
 * @since 3.2.0
 * @author shake
 */
object StorageRegionQueries {

    fun usedSpace(ctx: DSLContext, region: StorageRegionRecord): BigDecimal {
        val staging = Tables.STAGING_STORAGE
        return ctx.select(DSL.sum(staging.SIZE))
                .from(staging)
                .where(staging.REGION_ID.eq(region.id)).and(staging.ACTIVE.isTrue)
                .fetchOne(0, BigDecimal::class.java)
    }

}