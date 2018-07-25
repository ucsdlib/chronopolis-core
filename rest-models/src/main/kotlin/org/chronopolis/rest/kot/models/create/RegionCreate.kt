package org.chronopolis.rest.kot.models.create

import org.chronopolis.rest.kot.models.enums.DataType
import org.chronopolis.rest.kot.models.enums.StorageType
import org.chronopolis.rest.kot.models.enums.StorageUnit
import org.hibernate.validator.constraints.NotEmpty
import javax.validation.constraints.Min

data class RegionCreate(val note: String,
                        val node: String,
                        @get:Min(1) val capacity: Long,
                        val dataType: DataType,
                        val storageUnit: StorageUnit,
                        val storageType: StorageType,
                        @get:NotEmpty val replicationPath: String,
                        @get:NotEmpty val replicationServer: String,
                        val replicationUser: String?)