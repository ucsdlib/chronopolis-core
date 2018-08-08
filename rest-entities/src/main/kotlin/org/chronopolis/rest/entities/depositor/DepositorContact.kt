package org.chronopolis.rest.entities.depositor

import com.google.common.collect.ComparisonChain
import org.chronopolis.rest.entities.PersistableEntity
import org.chronopolis.rest.models.create.DepositorContactCreate
import java.util.Optional
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * Contact information for a Depositor
 *
 * @property contactName A name for this contact
 * @property contactEmail An email address, unique on ([Depositor], [DepositorContact])
 * @property contactPhone A phone number, previously validated
 * @property depositor The [Depositor] which this contact belongs to
 *
 * @author shake
 */
@Entity
class DepositorContact(
        var contactName: String = "",
        var contactPhone: String = "",
        var contactEmail: String = ""
) : PersistableEntity(), Comparable<DepositorContact> {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "depositor_id")
    var depositor: Depositor? = null

    override fun compareTo(other: DepositorContact): Int {
        return ComparisonChain.start()
                .compare(contactName, other.contactName)
                .compare(contactPhone, other.contactPhone)
                .compare(contactEmail, other.contactEmail)
                .result()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DepositorContact

        if (contactName != other.contactName) return false
        if (contactPhone != other.contactPhone) return false
        if (contactEmail != other.contactEmail) return false

        return true
    }

    override fun hashCode(): Int {
        var result = contactName.hashCode()
        result = 31 * result + contactPhone.hashCode()
        result = 31 * result + contactEmail.hashCode()
        return result
    }

}

fun fromRequest(request: DepositorContactCreate): Optional<DepositorContact> {
    return request.contactPhone.formatNumber()
            .map { DepositorContact(request.contactName, it, request.contactEmail) }
}

fun fromRequest(request: Collection<DepositorContactCreate>): Set<DepositorContact> {
    return request.map { req ->
        req.contactPhone.formatNumber()
                .map { DepositorContact(req.contactName, it, req.contactEmail) }
    }.filter { it.isPresent }
    .map { it.get() }
    .toSet()
}
