package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.Depositor
import org.chronopolis.rest.models.StorageRegion
import javax.validation.constraints.Min
import javax.validation.constraints.NotEmpty

/**
 * Request to create a [Bag]
 *
 * @property name The requested name to use
 * @property size The size of the [Bag]
 * @property totalFiles The number of files the [Bag] contains
 * @property storageRegion The [StorageRegion] the [Bag] resides in
 * @property location The relative location of the [Bag] in its [StorageRegion]
 * @property depositor The [Depositor] who owns the [Bag]
 * @author shake
 */
data class BagCreate(@get:NotEmpty var name: String = "",
                     @get:Min(1) var size: Long = 0,
                     @get:Min(1) var totalFiles: Long = 0,
                     @get:Min(1) var storageRegion: Long = 0,
                     @get:NotEmpty var location: String = "",
                     @get:NotEmpty var depositor: String = "")
