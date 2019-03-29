package org.chronopolis.rest.models.create

import org.chronopolis.rest.models.Bag
import org.chronopolis.rest.models.Depositor
import org.chronopolis.rest.models.Repair
import javax.validation.constraints.NotEmpty

/**
 * Request to create a [Repair]
 *
 * @property to the Node who the [Repair] is for
 * @property depositor the [Depositor] of the [Bag]
 * @property collection the name of the [Bag]
 * @property files the Set of files to repair, identified by their filenames
 * @author shake
 */
data class RepairCreate(@get:NotEmpty var to: String = "",
                        @get:NotEmpty var depositor: String = "",
                        @get:NotEmpty var collection: String = "",
                        @get:NotEmpty var files: Set<String> = mutableSetOf())
