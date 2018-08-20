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
 * @property proof
 * @property round
 * @property imsService
 * @property algorithm
 * @property imsHost
 * @property createDate
 * @property file
 * @property bag
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

        @OneToOne
        @JoinColumn(name = "file_id")
        var file: BagFile = BagFile()
) : PersistableEntity() {

    // move to constructor?
    @ManyToOne(fetch = FetchType.LAZY)
    lateinit var bag: Bag

}