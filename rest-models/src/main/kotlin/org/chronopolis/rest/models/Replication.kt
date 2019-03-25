package org.chronopolis.rest.models

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.models.enums.ReplicationStatus
import java.time.ZonedDateTime

data class Replication(val id: Long,
                       val createdAt: ZonedDateTime,
                       val updatedAt: ZonedDateTime,
                       val status: ReplicationStatus,
                       val bagLink: String,
                       val tokenLink: String,
                       val protocol: String,
                       val receivedTagFixity: String,
                       val receivedTokenFixity: String,
                       val node: String,
                       val bag: Bag) : Comparable<Replication> {

    override fun compareTo(other: Replication): Int {
        // minimum set of fields to compare
        return ComparisonChain.start()
                .compare(bag, other.bag)
                .compare(node, other.node)
                .compare(createdAt, other.createdAt)
                .result()
    }

}

