package org.chronopolis.rest.entities.storage

import org.chronopolis.rest.entities.PersistableEntity
import javax.persistence.Entity
import javax.persistence.OneToOne

@Entity
class ReplicationConfig(
        @OneToOne
        var region: StorageRegion = StorageRegion(),

        var path: String = "",
        var server: String = "",
        var username: String? = null
) : PersistableEntity()