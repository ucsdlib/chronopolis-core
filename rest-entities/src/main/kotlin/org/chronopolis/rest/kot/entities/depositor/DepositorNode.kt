package org.chronopolis.rest.kot.entities.depositor

import org.chronopolis.rest.kot.entities.Node
import org.chronopolis.rest.kot.entities.PersistableEntity
import java.io.Serializable
import javax.persistence.ManyToOne

/**
 * Entity representing a join table for Depositor <-> Node
 *
 * Will see if we need to impl equals/hashCode/toString
 * Also not sure if we really need Serializable
 *
 * @property depositor The [Depositor] to join on
 * @property node The [Node] to join on
 *
 * @author shake
 */
// @Entity
// @Table(name = "depositor_distribution")
class DepositorNode(
        @ManyToOne
        var depositor: Depositor? = null,

        @ManyToOne
        var node: Node? = null
) : PersistableEntity(), Serializable