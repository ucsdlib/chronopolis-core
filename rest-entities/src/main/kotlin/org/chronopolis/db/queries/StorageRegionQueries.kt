package org.chronopolis.db.queries

import org.chronopolis.db.generated.Tables
import org.chronopolis.db.generated.tables.StagingStorage
import org.chronopolis.db.generated.tables.StorageRegion
import org.chronopolis.db.generated.tables.records.StorageRegionRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL
import java.math.BigDecimal

/**
 * Object holding [StorageRegion] queries
 *
 * @since 3.2.0
 * @author shake
 */
object StorageRegionQueries {

    /**
     * Retrieve the amount of space that is being used in a [StorageRegion]. This is done by running
     * a sum over all [StagingStorage] for a [StorageRegion] rows which are active.
     *
     * @param ctx
     * @param region
     * @return sum of all active [StagingStorage] for a [StorageRegion] as a [BigDecimal]
     * @since 3.2.0
     */
    fun usedSpace(ctx: DSLContext, region: StorageRegionRecord): BigDecimal {
        val staging = Tables.STAGING_STORAGE
        return ctx.select(DSL.sum(staging.SIZE))
                .from(staging)
                .where(staging.REGION_ID.eq(region.id)).and(staging.ACTIVE.isTrue)
                .fetchOne(0, BigDecimal::class.java)
    }

}