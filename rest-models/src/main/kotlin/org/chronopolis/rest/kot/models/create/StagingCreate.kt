package org.chronopolis.rest.kot.models.create

import org.chronopolis.rest.kot.models.enums.StorageUnit
import org.hibernate.validator.constraints.NotBlank
import javax.validation.constraints.Min

data class StagingCreate(@get:NotBlank val location: String,
                         val storageRegion: Long,
                         @get:Min(1) val totalFiles: Long,
                         @get:Min(1) val size: Long,
                         val storageUnit: StorageUnit)