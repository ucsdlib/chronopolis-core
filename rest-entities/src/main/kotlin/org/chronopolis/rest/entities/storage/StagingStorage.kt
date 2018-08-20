package org.chronopolis.rest.entities.storage

import org.chronopolis.rest.entities.Bag
import org.chronopolis.rest.entities.DataFile
import org.chronopolis.rest.entities.TokenStore
import org.chronopolis.rest.entities.UpdatableEntity
import javax.persistence.Entity
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne

/**
 * Allocated storage in a [StorageRegion] used for distributing either a [Bag] or [TokenStore]
 *
 * @property region The [StorageRegion] which space is allocated in
 * @property bag The [Bag] associated with this space
 * @property size The amount of space which has been allocated/needed when distributing
 * @property totalFiles The number of files staged for distribution
 * @property path The relative path to this data set in the [StorageRegion]
 * @property active Whether or not this data is still available
 * @property file A file used for validation during distributions of the data
 *
 * @author shake
 */
@Entity
class StagingStorage(
        @ManyToOne
        var region: StorageRegion = StorageRegion(),
        @ManyToOne
        var bag: Bag = Bag(),
        var size: Long = 0L,
        var totalFiles: Long = 0L,
        var path: String = "",
        var active: Boolean = true,

        // because this is abstract I think it needs to be nullable :/
        // we could make DataFile open but I'd rather note
        // maybe move to a lateinit
        @ManyToOne
        @JoinColumn(name = "file_id")
        var file: DataFile? = null
) : UpdatableEntity() {

    // Helper function for more fluent verbage
    fun isActive() = active

}