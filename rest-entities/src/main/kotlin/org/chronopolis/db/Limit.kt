package org.chronopolis.db

/**
 * Encapsulate parameters used for the limit and offset of a database query
 *
 * @property limit the limit to pass to the query
 * @property offset the offset to pass to the query
 * @since 3.2.0
 * @author shake
 */
data class Limit(val limit: Int, val offset: Int)