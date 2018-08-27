package org.chronopolis.rest.entities

import javax.persistence.CascadeType.MERGE
import javax.persistence.CascadeType.PERSIST
import javax.persistence.DiscriminatorValue
import javax.persistence.Entity
import javax.persistence.FetchType.EAGER
import javax.persistence.OneToOne

@Entity
@DiscriminatorValue("BAG")
class BagFile : DataFile() {
    @OneToOne(mappedBy = "file", cascade = [MERGE, PERSIST], orphanRemoval = true, fetch = EAGER)
    var token: AceToken? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BagFile

        if (bag != other.bag) return false
        if (filename != other.filename) return false

        return true
    }

    override fun hashCode(): Int {
        return 31 * filename.hashCode() + bag.hashCode()
    }
}