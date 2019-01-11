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