package org.chronopolis.rest.models

/**
 * A File belonging to a [Bag]
 *
 * @property filename the name of the file, relative to the root of the [Bag]
 * @property size the size of the file in bytes
 * @property fixities the [Fixity] values which have been registered to the [File]
 * @property bag the id of the [Bag] which this [File] belongs to
 *
 * @author shake
 */
data class File(val filename: String,
                val size: Long,
                val fixities: Set<Fixity>,
                val bag: Long)