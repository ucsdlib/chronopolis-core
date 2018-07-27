package org.chronopolis.rest.kot.entities.depositor

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.kot.entities.PersistableEntity
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class DepositorContact(
        var contactName: String = "",
        var contactPhone: String = "",
        var contactEmail: String = ""
) : PersistableEntity(), Comparable<DepositorContact> {

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "depositor_id")
    lateinit var depositor: Depositor

    override fun compareTo(other: DepositorContact): Int {
        return ComparisonChain.start()
                .compare(contactName, other.contactName)
                .compare(contactPhone, other.contactPhone)
                .compare(contactEmail, other.contactEmail)
                .result()
    }

}

