package org.chronopolis.rest.entities.projections

import com.querydsl.core.annotations.QueryProjection
import org.chronopolis.rest.entities.Bag
import org.chronopolis.rest.entities.Replication
import org.chronopolis.rest.entities.storage.StagingStorage
import org.chronopolis.rest.models.enums.ReplicationStatus
import java.time.ZonedDateTime

/**
 * View for a [Replication] which joins a [Bag] and its [StagingStorage]
 *
 * @author shake
 */
class ReplicationView @QueryProjection constructor(
        val id: Long,
        val createdAt: ZonedDateTime,
        val updatedAt: ZonedDateTime,
        val status: ReplicationStatus,
        val bagLink: String,
        val tokenLink: String,
        val protocol: String,
        val receivedTagFixity: String?,
        val receivedTokenFixity: String?,
        val node: String,
        val bag: CompleteBag
) {
    override fun toString(): String {
        return "ReplicationView[id=$id;createdAt=$createdAt;updatedAt=$updatedAt;status=$status;" +
                "bagLink=$bagLink;tokenLink=$tokenLink;protocol=$protocol;receivedTagFixity=" +
                "$receivedTagFixity;receivedTokenFixity=$receivedTokenFixity;node=$node;bag=$bag]"
    }
}
