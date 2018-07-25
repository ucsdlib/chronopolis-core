package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.ReplicationStatus
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
                       val bag: Bag)

