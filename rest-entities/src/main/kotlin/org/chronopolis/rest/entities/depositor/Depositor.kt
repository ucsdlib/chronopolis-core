package org.chronopolis.rest.entities.depositor

import org.chronopolis.rest.entities.Node
import org.chronopolis.rest.entities.UpdatableEntity
import org.hibernate.annotations.NaturalId
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.OneToMany

/**
 * A Depositor in Chronopolis
 *
 * @property namespace The namespace of the Depositor. Must be unique.
 * @property sourceOrganization The source organization the Depositor belongs to.
 * @property organizationAddress The address of the source organization.
 * @property contacts A set of [DepositorContact]s which belong to the depositor. Note: This should
 * be updated so that a [Depositor] is the owner of the relationship.
 * @property nodeDistributions A set of [Node]s which control where data goes to for this
 * depositor. Might be worth looking at how this relationship is setup to make ownership better in
 * Kotlin.
 *
 * @author shake
 */
@Entity
class Depositor(
        @NaturalId
        var namespace: String = "",

        var sourceOrganization: String = "",

        var organizationAddress: String = ""
) : UpdatableEntity(), Comparable<Depositor> {

    @OneToMany(mappedBy = "depositor", cascade = [CascadeType.ALL], orphanRemoval = true)
    lateinit var contacts: MutableSet<DepositorContact>

    @JoinTable(name = "depositor_distribution",
            joinColumns = [JoinColumn(name = "depositor_id")],
            inverseJoinColumns = [JoinColumn(name = "node_id")])
    @ManyToMany(cascade = [CascadeType.ALL])
    lateinit var nodeDistributions: MutableSet<Node>

    // Helpers for adding/removing contacts and distributions?

    fun addContact(contact: DepositorContact) {
        contact.depositor = this
        contacts.add(contact)
    }

    fun removeContact(contact: DepositorContact) {
        contacts.remove(contact)
        contact.depositor = null
    }

    fun addNodeDistribution(node: Node) {
        nodeDistributions.add(node)
    }

    fun removeNodeDistribution(node: Node) {
        nodeDistributions.remove(node)
    }

    override fun compareTo(other: Depositor): Int {
        return namespace.compareTo(other.namespace)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Depositor

        if (namespace != other.namespace) return false

        return true
    }

    override fun hashCode(): Int {
        return namespace.hashCode()
    }
}

