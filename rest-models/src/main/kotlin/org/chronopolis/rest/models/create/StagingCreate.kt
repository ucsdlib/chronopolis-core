package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.StagingStorage
import org.chronopolis.rest.models.StorageRegion
import org.chronopolis.rest.models.enums.StorageUnit
import org.hibernate.validator.constraints.NotBlank
import javax.validation.constraints.Min

/**
 * Request to create [StagingStorage] for a [Bag]
 *
 * @property location the relative location of the staged data within the [StorageRegion]
 * @property validationFile the name of the [DataFile] used to validate transfers
 * @property storageRegion the [StorageRegion] the staged data resides in
 * @property totalFiles the number of files staged
 * @property size the amount of storage capacity used by the staged data
 * @property storageUnit the [StorageUnit] used in conjunction with the [size] to define the usage
 */
data class StagingCreate(@get:NotBlank var location: String = "",
                         @get:NotBlank var validationFile: String = "/tagmanifest-sha256.txt",
                         @get:Min(1) var storageRegion: Long = -1,
                         @get:Min(1) var totalFiles: Long = -1,
                         @get:Min(1) var size: Long = -1,
                         var storageUnit: StorageUnit = StorageUnit.B)