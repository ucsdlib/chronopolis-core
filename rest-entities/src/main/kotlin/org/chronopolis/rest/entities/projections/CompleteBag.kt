package org.chronopolis.rest.entities.projections

import com.querydsl.core.annotations.QueryProjection
import org.chronopolis.rest.entities.Bag
import org.chronopolis.rest.models.enums.BagStatus
import java.time.ZonedDateTime

/**
 * A full view of a [Bag], including join on its StagingStorage
 *
 * @author shake
 */
@Suppress("MemberVisibilityCanBePrivate")
class CompleteBag @QueryProjection constructor(
        val id: Long,
        val name: String,
        val creator: String,
        val size: Long,
        val totalFiles: Long,
        val status: BagStatus,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
        val depositor: String,
        val replicatingNodes: Set<String>,
        val storage: Map<String, StagingView>
) {
    override fun toString(): String {
        return "CompleteBag[\nid=$id;\nname=$name;\ncreator=$creator;\nsize=$size;\n" +
                "totalFiles=$totalFiles;\nstatus=$status;\ncreatedAt=$createdAt;\n" +
                "updatedAt=$updatedAt;\ndepositor=$depositor;replicatingNodes=$replicatingNodes;" +
                "storage=$storage;]"
    }
}