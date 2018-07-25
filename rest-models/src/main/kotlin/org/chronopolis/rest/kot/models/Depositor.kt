package org.chronopolis.rest.kot.models

import java.time.ZonedDateTime

data class Depositor(val id: Long,
                     val namespace: String,
                     val sourceOrganization: String,
                     val organizationAddress: String,
                     val createdAt: ZonedDateTime,
                     val updatedAt: ZonedDateTime,
                     val replicatingNodes: Set<String>,
                     val contacts: Set<DepositorContact>)
