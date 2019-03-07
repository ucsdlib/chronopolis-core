package org.chronopolis.rest.entities.projections

import com.querydsl.core.annotations.QueryProjection
import java.util.Date

/**
 * Normal [AceToken] entity but with the filename joined
 *
 * @since 3.1.0
 * @author shake
 */
class AceToken @QueryProjection constructor(
        val id: Long,
        val bagId: Long,
        val round: Long,
        val imsHost: String,
        val imsService: String,
        val algorithm: String,
        val proof: String,
        val createDate: Date,
        val filename: String
) {
    override fun toString(): String {
        return "AceToken[id=$id,bag_id=$bagId,filename=$filename]"
    }
}