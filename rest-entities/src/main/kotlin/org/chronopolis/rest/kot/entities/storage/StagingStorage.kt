package org.chronopolis.rest.kot.entities.storage

import org.chronopolis.rest.kot.entities.Bag
import org.chronopolis.rest.kot.entities.UpdatableEntity
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.FetchType
import javax.persistence.ManyToMany
import javax.persistence.ManyToOne
import javax.persistence.OneToMany

@Entity
class StagingStorage(
        @get:ManyToOne
        var region: StorageRegion = StorageRegion(),

        var size: Long = 0L,
        var totalFiles: Long = 0L,
        var path: String = "",
        var active: Boolean = true
) : UpdatableEntity() {
    // Still super sloppy from previous impl
    // allegedly only one of the bag_storage or token_storage will be used depending on the type of
    // storage this is associated with... should probably update the schema to have a better
    // understanding of the world
    @get:ManyToMany(fetch = FetchType.LAZY, mappedBy = "bagStorage")
    lateinit var bags: MutableSet<Bag>

    @get:ManyToMany(fetch = FetchType.LAZY, mappedBy = "tokenStorage")
    lateinit var tokens: MutableSet<Bag>

    @get:OneToMany(mappedBy = "storage", cascade = [CascadeType.ALL], fetch = FetchType.EAGER)
    lateinit var fixities: MutableSet<Fixity>

    // Helper function for more fluent verbage

    fun addFixity(fixity: Fixity) {
        TODO()
    }

    fun isActive() = active

}