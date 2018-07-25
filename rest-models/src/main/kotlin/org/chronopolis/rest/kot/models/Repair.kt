package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.AuditStatus
import org.chronopolis.rest.kot.models.enums.FulfillmentType
import org.chronopolis.rest.kot.models.enums.RepairStatus
import java.time.ZonedDateTime

data class Repair(val id: Long,
                  val createdAt: ZonedDateTime,
                  val updatedAt: ZonedDateTime,
                  val cleaned: Boolean,
                  val replaced: Boolean,
                  val validated: Boolean,
                  val audit: AuditStatus,
                  val status: RepairStatus,
                  val to: String,
                  val requester: String,
                  val depositor: String,
                  val collection: String,
                  val files: List<String>,

        // Should we have any extra information in the credentials?
        // like if the fulfilling node has cleaned it yet?
                  val from: String,
                  val type: FulfillmentType,
                  val credentials: FulfillmentStrategy)

