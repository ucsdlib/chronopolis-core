package org.chronopolis.rest.models.update

import org.chronopolis.rest.models.StorageRegion
import org.chronopolis.rest.models.enums.DataType
import org.chronopolis.rest.models.enums.StorageType
import org.chronopolis.rest.models.enums.StorageUnit
import javax.validation.constraints.Min

/**
 * Possible updates to a [StorageRegion]
 *
 * @property capacity the storage capacity of the [StorageRegion], used with the [storageUnit]
 * @property note a note to further identify the [StorageRegion]
 * @property storageUnit the [StorageUnit] used to conjunction with the [capacity]
 * @property dataType the [DataType] identifying what the [StorageRegion] is used for
 * @property storageType the [StorageType] defining what type of storage this is
 *
 * @author shake
 */
data class RegionUpdate(@get:Min(1) var capacity: Long = 0,
                        var note: String = "",
                        var storageUnit: StorageUnit = StorageUnit.B,
                        var dataType: DataType = DataType.BAG,
                        var storageType: StorageType = StorageType.LOCAL)