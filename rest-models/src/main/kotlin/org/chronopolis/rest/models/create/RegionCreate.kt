package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Replication
import org.chronopolis.rest.models.StorageRegion
import org.chronopolis.rest.models.enums.DataType
import org.chronopolis.rest.models.enums.StorageType
import org.chronopolis.rest.models.enums.StorageUnit
import org.hibernate.validator.constraints.NotEmpty
import javax.validation.constraints.Min

/**
 * A request to create a [StorageRegion]
 *
 * @property note a note to further identify the [StorageRegion]
 * @property node the Chronopolis Node which the [StorageRegion] belongs to
 * @property capacity the storage capacity of the [StorageRegion], used with the [storageUnit]
 * @property dataType the [DataType] identifying what the [StorageRegion] is used for
 * @property storageUnit the [StorageUnit] used to conjunction with the [capacity]
 * @property storageType the [StorageType] defining what type of storage this is
 * @property replicationPath the path to be used when creating [Replication]s
 * @property replicationServer the server to be used when creating [Replication]s
 * @property replicationUser the user to use when creating [Replication]s, or null
 * @author shake
 */
data class RegionCreate(var note: String = "",
                        @get:NotEmpty var node: String = "",
                        @get:Min(1) var capacity: Long = 0,
                        var dataType: DataType = DataType.BAG,
                        var storageUnit: StorageUnit = StorageUnit.B,
                        var storageType: StorageType = StorageType.LOCAL,
                        @get:NotEmpty var replicationPath: String = "",
                        @get:NotEmpty var replicationServer: String = "",
                        var replicationUser: String? = null) {

    /**
     * Return a normalized representation of the [capacity] by multiplying it by 1000 raised to the
     * power of the [storageUnit]
     */
    fun normalizedCapacity(): Double {
        return capacity * Math.pow(1000.0, storageUnit.power.toDouble())
    }
}