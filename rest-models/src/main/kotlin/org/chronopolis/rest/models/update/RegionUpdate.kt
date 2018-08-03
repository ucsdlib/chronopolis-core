package org.chronopolis.rest.models.update

import org.chronopolis.rest.models.enums.DataType
import org.chronopolis.rest.models.enums.StorageType
import org.chronopolis.rest.models.enums.StorageUnit
import javax.validation.constraints.Min

data class RegionUpdate(@get:Min(1) val capacity: Long,
                        val note: String,
                        val storageUnit: StorageUnit,
                        val dataType: DataType,
                        val storageType: StorageType)