package org.chronopolis.rest.entities.repair

import org.chronopolis.rest.entities.Bag
import org.chronopolis.rest.entities.Node
import org.chronopolis.rest.entities.UpdatableEntity
import org.chronopolis.rest.models.enums.AuditStatus
import org.chronopolis.rest.models.enums.FulfillmentType
import org.chronopolis.rest.models.enums.RepairStatus
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class Repair(
        @ManyToOne
        var bag: Bag = Bag(),

        @ManyToOne
        @JoinColumn(name = "to_node")
        var to: Node = Node(),

        @ManyToOne
        @JoinColumn(name = "from_node")
        var from: Node? = null,

        @Enumerated(value = EnumType.STRING)
        var status: RepairStatus = RepairStatus.REQUESTED,

        @Enumerated(value = EnumType.STRING)
        var audit: AuditStatus = AuditStatus.PRE,

        @Enumerated(value = EnumType.STRING)
        var type: FulfillmentType? = null,

        @OneToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
        var strategy: Strategy? = null,

        var requester: String = "",
        var cleaned: Boolean = false,
        var replaced: Boolean = false,
        var validated: Boolean = false
) : UpdatableEntity() {

        @OneToMany(mappedBy = "repair", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        lateinit var files: MutableSet<RepairFile>

        fun addFilesFromRequest(toAdd: Set<String>) {
                toAdd.forEach { files.add(RepairFile(this, it)) }
        }
}