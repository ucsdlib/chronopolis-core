package org.chronopolis.rest.kot.entities.depositor

import org.chronopolis.rest.kot.entities.UpdatableEntity
import org.hibernate.annotations.NaturalId
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.OneToMany

@Entity
class Depositor(
        @get:NaturalId
        var namespace: String = "",

        var sourceOrganization: String = "",

        var organizationAddress: String = ""
) : UpdatableEntity(), Comparable<Depositor> {

    @get:OneToMany(mappedBy = "depositor", cascade = [CascadeType.ALL], orphanRemoval = true)
    lateinit var contacts: Set<DepositorContact>

    @get:OneToMany(mappedBy = "depositor", cascade = [CascadeType.ALL], orphanRemoval = true)
    lateinit var nodeDistributions: Set<DepositorNode>

    // Helpers for adding/removing contacts and distributions?

    override fun compareTo(other: Depositor): Int {
        return namespace.compareTo(other.namespace)
    }
}

