package org.chronopolis.rest.kot.entities.repair

import org.chronopolis.rest.kot.entities.Bag
import org.chronopolis.rest.kot.entities.Node
import org.chronopolis.rest.kot.entities.UpdatableEntity
import org.chronopolis.rest.kot.models.enums.AuditStatus
import org.chronopolis.rest.kot.models.enums.FulfillmentType
import org.chronopolis.rest.kot.models.enums.RepairStatus
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
        @get:ManyToOne
        var bag: Bag = Bag(),

        @get:ManyToOne
        @get:JoinColumn(name = "to_node")
        var to: Node = Node(),

        @get:ManyToOne
        @get:JoinColumn(name = "from_node")
        var from: Node? = null,

        @get:Enumerated(value = EnumType.STRING)
        var status: RepairStatus = RepairStatus.REQUESTED,

        @get:Enumerated(value = EnumType.STRING)
        var audit: AuditStatus = AuditStatus.PRE,

        @get:Enumerated(value = EnumType.STRING)
        var type: FulfillmentType? = null,

        @get:OneToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
        var strategy: Strategy? = null,

        var requester: String = "",
        var cleaned: Boolean = false,
        var replaced: Boolean = false,
        var validated: Boolean = false
) : UpdatableEntity() {

        @get:OneToMany(mappedBy = "repair", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
        lateinit var files: MutableSet<RepairFile>

        fun addFilesFromRequest(toAdd: Set<String>) {
                toAdd.forEach { files.add(RepairFile(this, it)) }
        }
}