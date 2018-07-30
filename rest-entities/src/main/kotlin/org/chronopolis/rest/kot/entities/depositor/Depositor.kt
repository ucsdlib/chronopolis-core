package org.chronopolis.rest.kot.entities.depositor

import org.chronopolis.rest.kot.entities.Node
import org.chronopolis.rest.kot.entities.UpdatableEntity
import org.hibernate.annotations.NaturalId
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class Depositor(
        @NaturalId
        var namespace: String = "",

        var sourceOrganization: String = "",

        var organizationAddress: String = ""
) : UpdatableEntity(), Comparable<Depositor> {

    @OneToMany(mappedBy = "depositor", cascade = [CascadeType.ALL], orphanRemoval = true)
    lateinit var contacts: MutableSet<DepositorContact>

    @OneToMany(mappedBy = "depositor", cascade = [CascadeType.ALL], orphanRemoval = true)
    lateinit var nodeDistributions: MutableSet<DepositorNode>

    // Helpers for adding/removing contacts and distributions?

    fun addContact(contact: DepositorContact) {
        contact.depositor = this
        contacts.add(contact)
    }

    fun removeContact(contact: DepositorContact) {
        contacts.remove(contact)
    }

    fun addNodeDistribution(node: Node) {
        val dn = DepositorNode(this, node)
        nodeDistributions.add(dn)
    }

    fun removeNodeDistribution(node: Node) {
        val dn = DepositorNode(this, node)
        nodeDistributions.remove(dn)
        node.depositorDistributions.remove(dn)
        // todo null out dn after?
    }

    override fun compareTo(other: Depositor): Int {
        return namespace.compareTo(other.namespace)
    }
}

