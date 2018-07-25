package org.chronopolis.rest.kot.entities

import java.util.Date
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

@Entity
class AceToken(
        var filename: String = "",
        var proof: String = "",
        var round: Long = -1,
        var imsService: String = "",
        var algorithm: String = "",
        var imsHost: String = "",
        var createDate: Date = Date()
) : PersistableEntity() {

    @get:JoinColumn(name = "bag")
    @get:ManyToOne(fetch = FetchType.LAZY)
    lateinit var bag: Bag

}