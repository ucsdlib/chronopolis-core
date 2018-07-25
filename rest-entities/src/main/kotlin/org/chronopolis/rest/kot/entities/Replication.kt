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
        @get:Enumerated(EnumType.STRING)
        var status: ReplicationStatus = ReplicationStatus.PENDING,

        @get:ManyToOne
        var node: Node = Node(),

        @get:ManyToOne(cascade = [CascadeType.MERGE])
        var bag: Bag = Bag(),

        var bagLink: String = "",
        var tokenLink: String = "",
        var protocol: String = "",
        var receivedTagFixity: String = "",
        var receivedTokenFixity: String = ""
) : UpdatableEntity() {

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