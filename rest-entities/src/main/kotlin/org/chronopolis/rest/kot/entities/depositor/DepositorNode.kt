package org.chronopolis.rest.kot.entities.depositor

import org.chronopolis.rest.kot.entities.Node
import org.chronopolis.rest.kot.entities.PersistableEntity
import java.io.Serializable
import javax.persistence.Entity
import javax.persistence.ManyToOne
import javax.persistence.Table

/**
 * Entity representing a join table for Depositor <-> Node
 *
 * Will see if we need to impl equals/hashCode/toString
 * Also not sure if we really need Serializable
 *
 * @author shake
 */
@Entity
@Table(name = "depositor_distribution")
class DepositorNode(
        @get:ManyToOne
        var depositor: Depositor = Depositor(),

        @get:ManyToOne
        var node: Node = Node()
) : PersistableEntity(), Serializable