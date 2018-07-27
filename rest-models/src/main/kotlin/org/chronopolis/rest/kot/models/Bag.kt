package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.BagStatus
import java.time.ZonedDateTime

data class Bag(val id: Long,
               val size: Long,
               val totalFiles: Long,
               val bagStorage: StagingStorage?,
               val tokenStorage: StagingStorage?,
               val createdAt: ZonedDateTime,
               val updatedAt: ZonedDateTime,
               val name: String,
               val creator: String,
               val depositor: String,
               val status: BagStatus,
               val replicatingNodes: Set<String>)
