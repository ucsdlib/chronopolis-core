package org.chronopolis.rest.kot.entities

import org.chronopolis.rest.kot.models.enums.ReplicationStatus
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.ManyToOne
import javax.persistence.PreUpdate

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
        val bagFixities = bag.bagStorage
        val tokenFixities = bag.tokenStorage

        val tagMatch = bagFixities.flatMap { it.fixities }
                .any { it.value.equals(receivedTagFixity, true) }

        val tokenMatch = tokenFixities.flatMap { it.fixities }
                .any { it.value.equals(receivedTokenFixity, true) }

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
        }
    }
}