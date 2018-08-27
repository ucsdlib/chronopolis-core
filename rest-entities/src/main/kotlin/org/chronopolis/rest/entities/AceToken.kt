package org.chronopolis.rest.entities

import java.util.Date
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.OneToOne

/**
 * AceToken connected to a single [BagFile]
 *
 * @property proof the proof returned by the IMS
 * @property round the round returned by the IMS
 * @property imsService the digest service used by the IMS
 * @property algorithm the digest algorithm used by the IMS
 * @property imsHost the hostname of the IMS
 * @property createDate the create date of the Ace Token returned by the IMS
 * @property bag the corresponding [Bag]
 * @property file the corresponding [BagFile]
 *
 * @author shake
 */
@Entity
class AceToken(
        var proof: String = "",
        var round: Long = -1,
        var imsService: String = "",
        var algorithm: String = "",
        var imsHost: String = "",
        var createDate: Date = Date(),

        @ManyToOne(fetch = FetchType.LAZY)
        var bag: Bag = Bag(),

        @OneToOne
        @JoinColumn(name = "file_id")
        var file: BagFile = BagFile()
) : PersistableEntity() {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AceToken

        if (proof != other.proof) return false
        if (round != other.round) return false
        if (imsService != other.imsService) return false
        if (algorithm != other.algorithm) return false
        if (imsHost != other.imsHost) return false
        if (createDate != other.createDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = proof.hashCode()
        result = 31 * result + round.hashCode()
        result = 31 * result + imsService.hashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + imsHost.hashCode()
        result = 31 * result + createDate.hashCode()
        return result
    }

}