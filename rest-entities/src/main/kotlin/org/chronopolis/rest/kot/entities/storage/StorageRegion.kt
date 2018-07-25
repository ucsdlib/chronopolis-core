package org.chronopolis.rest.kot.entities.storage

import org.chronopolis.rest.kot.entities.Node
import org.chronopolis.rest.kot.entities.UpdatableEntity
import org.chronopolis.rest.kot.models.enums.DataType
import org.chronopolis.rest.kot.models.enums.StorageType
import javax.persistence.CascadeType
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.ManyToOne
import javax.persistence.OneToMany
import javax.persistence.OneToOne

@Entity
class StorageRegion(
        @get:ManyToOne
        var node: Node = Node(),

        @get:Enumerated(value = EnumType.STRING)
        var dataType: DataType = DataType.BAG,

        @get:Enumerated(value = EnumType.STRING)
        var storageType: StorageType = StorageType.LOCAL,

        @get:OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
        var storage: Set<StagingStorage> = emptySet(),

        @get:OneToOne(mappedBy = "region", cascade = [CascadeType.ALL])
        var replicationConfig: ReplicationConfig = ReplicationConfig(),

        var capacity: Long = 0,
        var note: String = ""
) : UpdatableEntity()