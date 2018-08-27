package org.chronopolis.rest.entities

import org.chronopolis.rest.models.enums.ReplicationStatus
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.PreUpdate

/**
 * Replication in chronopolis beep
 *
 * @author shake
 */
@Entity
class Replication(
        @Enumerated(EnumType.STRING)
        var status: ReplicationStatus = ReplicationStatus.PENDING,

        @ManyToOne
        var node: Node = Node(),

        @ManyToOne(cascade = [CascadeType.MERGE])
        var bag: Bag = Bag(),

        var bagLink: String = "",
        var tokenLink: String = "",
        var protocol: String = "",
        var receivedTagFixity: String? = null,
        var receivedTokenFixity: String? = null
) : UpdatableEntity() {

    fun checkTransferred() = {
        // todo: test when storage isEmpty
        // this is still a bit awkward
        // considering passing the StagingStorage directly to this function rather than trying
        // to iterate through and match
        val tagMatch = bag.storage.filter { it.file.dtype == "BAG" }
                .first { it.isActive() }
                .file.fixities.any { it.value.equals(receivedTagFixity, true) }

        val tokenMatch = bag.storage.filter { it.file.dtype == "TOKEN_STORE" }
                .first { it.isActive() }
                .file.fixities.any { it.value.equals(receivedTokenFixity, true) }

        if (status.isOngoing() && tagMatch && tokenMatch) {
            status = ReplicationStatus.TRANSFERRED
        }
    }

    @PreUpdate
    protected fun updateReplication() {
        if (status.isFailure()) {
            return
        }

        if (status == ReplicationStatus.SUCCESS) {
            // the filter should bring us to a single object, which we then mark as replicated
            bag.distributions
                    .filter { it.node == node }
                    .forEach { it.status = BagDistributionStatus.REPLICATE }

            bag.onUpdateBag()
        }
    }
}