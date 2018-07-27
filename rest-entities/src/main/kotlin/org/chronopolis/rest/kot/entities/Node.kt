package org.chronopolis.rest.kot.entities

import org.chronopolis.rest.kot.entities.depositor.DepositorNode
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany

/**
 * Representation of a Node in Chronopolis
 *
 * todo: lateinit Sets?
 * todo: remove password/enabled... maybe rename username
 *
 * @author shake
 */
@Entity
class Node(
        @get:OneToMany(mappedBy = "node")
        var replications: Set<Replication> = emptySet(),

        @get:OneToMany(mappedBy = "node")
        var restorations: Set<Restoration> = emptySet(),

        @get:OneToMany(mappedBy = "node", cascade = [CascadeType.ALL], orphanRemoval = true)
        var depositorDistributions: Set<DepositorNode> = emptySet(),

        var username: String = "",
        var password: String = "",
        var enabled: Boolean = true
) : PersistableEntity()