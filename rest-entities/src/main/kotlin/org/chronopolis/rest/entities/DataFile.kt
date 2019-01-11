package org.chronopolis.rest.entities

import org.chronopolis.rest.entities.storage.Fixity
import javax.persistence.CascadeType.ALL
import javax.persistence.Column
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.FetchType.EAGER
import javax.persistence.FetchType.LAZY
import javax.persistence.Inheritance
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.PrePersist
import javax.persistence.Table

/**
 * A generalized class for files which may be stored in Chronopolis. At the moment there are two
 * separate "types" - BAG and TOKEN_STORE. Each type is distinct in the function it provides, which
 * means that they are contained in two separate classes: [BagFile] and [TokenStore]. The [DataFile]
 * contains the common columns, which in reality is just about everything...
 *
 * @author shake
 */
@Entity
@Inheritance
@Table(name = "file")
@DiscriminatorColumn(name = "dtype")
abstract class DataFile(
        var filename: String = "",
        var size: Long = 0,

        @ManyToOne(fetch = LAZY)
        var bag: Bag = Bag()
) : UpdatableEntity() {

    @Column(insertable = false, updatable = false)
    var dtype: String? = null

    @OneToMany(mappedBy = "file", cascade = [ALL], fetch = EAGER, orphanRemoval = true)
    var fixities: MutableSet<Fixity> = mutableSetOf()

    fun addFixity(fixity: Fixity) {
        fixities.add(fixity)
        fixity.file = this
    }

    fun rmFixity(fixity: Fixity) {
        fixities.remove(fixity)
        fixity.file = null
    }

    @PrePersist
    fun checkLeading() {
        if (!filename.startsWith("/")) {
            this.filename = "/$filename"
        }
    }

}