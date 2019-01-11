package org.chronopolis.rest.models

import java.time.ZonedDateTime

/**
 * A File belonging to a [Bag]
 *
 * @property id the id of the file
 * @property filename the name of the file, relative to the root of the [Bag]
 * @property size the size of the file in bytes
 * @property fixities the [Fixity] values which have been registered to the [File]
 * @property bag the id of the [Bag] which this [File] belongs to
 * @property createdAt the date the [File] was created in the Chronopolis
 * @property updatedAt the most recent date of update for the [File]
 *
 * @author shake
 */
data class File(val id: Long,
                val filename: String,
                val size: Long,
                val fixities: Set<Fixity>,
                val bag: Long,
                val createdAt: ZonedDateTime,
                val updatedAt: ZonedDateTime)