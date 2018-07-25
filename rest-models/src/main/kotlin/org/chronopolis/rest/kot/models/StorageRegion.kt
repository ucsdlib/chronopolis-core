package org.chronopolis.rest.kot.models

import org.chronopolis.rest.kot.models.enums.DataType
import org.chronopolis.rest.kot.models.enums.StorageType

data class StorageRegion(val id: Long,
                         val node: String,
                         val note: String,
                         val capacity: Long,
                         val dataType: DataType,
                         val storageType: StorageType,
                         val replicationConfig: ReplicationConfig)

