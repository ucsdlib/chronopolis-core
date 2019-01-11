package org.chronopolis.rest.models

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.models.enums.BagStatus
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
               val replicatingNodes: Set<String>) : Comparable<Bag> {

    override fun compareTo(other: Bag): Int {
        return ComparisonChain.start()
                .compare(name, other.name)
                .compare(depositor, other.depositor)
                .result()
    }

}
