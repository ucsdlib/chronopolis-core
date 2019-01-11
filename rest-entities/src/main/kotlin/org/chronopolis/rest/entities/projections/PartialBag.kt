package org.chronopolis.rest.entities.projections

import com.querydsl.core.annotations.QueryProjection
import org.chronopolis.rest.entities.Bag
import org.chronopolis.rest.models.enums.BagStatus
import java.time.ZonedDateTime

/**
 * A partial view of a [Bag], with joins on the Depositor (namespace) and
 * BagDistribution/Node (node.username)
 *
 * @author shake
 */
@Suppress("MemberVisibilityCanBePrivate")
class PartialBag @QueryProjection constructor(
        val id: Long,
        val name: String,
        val creator: String,
        val size: Long,
        val totalFiles: Long,
        val status: BagStatus,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
        val depositor: String,
        val replicatingNodes: Set<String>
) {
    override fun toString(): String {
        return "PartialBag[id=$id;name=$name;creator=$creator;size=$size;" +
                "totalFiles=$totalFiles;status=$status;createdAt=$createdAt;" +
                "updatedAt=$updatedAt;depositor=$depositor;replicatingNodes=$replicatingNodes]"
    }
}