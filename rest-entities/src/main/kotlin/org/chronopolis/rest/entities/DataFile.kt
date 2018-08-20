package org.chronopolis.rest.entities

import org.chronopolis.rest.entities.storage.Fixity
import java.time.ZonedDateTime
import javax.persistence.CascadeType.ALL
import javax.persistence.DiscriminatorColumn
import javax.persistence.Entity
import javax.persistence.FetchType.LAZY
import javax.persistence.Inheritance
import javax.persistence.JoinColumn
import javax.persistence.JoinTable
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToOne
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
        var createdAt: ZonedDateTime = ZonedDateTime.now(),
        var filename: String = "",
        var size: Long = 0,

        @ManyToOne
        var bag: Bag = Bag()
) : PersistableEntity() {

    @OneToOne(mappedBy = "file", cascade = [ALL], optional = true, fetch = LAZY)
    var token: AceToken? = null

    @JoinTable(name = "file_fixity",
            joinColumns = [(JoinColumn(name = "file_id"))],
            inverseJoinColumns = [JoinColumn(name = "fixity_id")])
    @ManyToMany(cascade = [ALL])
    var fixities: MutableSet<Fixity> = mutableSetOf()

}