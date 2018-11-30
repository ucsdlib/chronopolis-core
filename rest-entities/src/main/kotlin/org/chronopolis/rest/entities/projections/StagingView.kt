package org.chronopolis.rest.entities.projections

import com.querydsl.core.annotations.QueryProjection

/**
 * Full view into staging storage including joining on the File (filename)
 *
 * @author shake
 */
class StagingView @QueryProjection constructor(
        val id: Long,
        val path: String,
        val type: String,
        val region: Long,
        val active: Boolean,
        val totalFiles: Long
) {
    override fun toString(): String {
        return "FullStaging[id=$id;path=$path;type=$type;region=$region;" +
                "active=$active;totalFiles=$totalFiles]"
    }
}
