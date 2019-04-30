package org.chronopolis.db

import org.chronopolis.db.binding.Pageable
import org.jooq.Condition
import org.jooq.OrderField
import org.jooq.Record
import java.util.Optional

/**
 * Definition of a DAO in the Chronopolis context. As this serves as the base for a query object,
 * we provide a few standard crud options:
 * [Dao.findOne] finding a single database row
 * [Dao.findAll/3] finding all rows
 * [Dao.findAll/1] finding a paged set of rows
 *
 * @property T the [Record] type which will be queried from the implementing class
 * @property ID the type of the id field which can be used when querying
 * @since 3.2.0
 * @author shake
 */
interface Dao<T : Record, ID> {

    /**
     * Find all rows which match a given [Pageable].
     *
     * @param pageable The [Pageable] containing query parameters and order/limit
     * @return a [List] of all all returned rows, mapped to [T]
     */
    fun findAll(pageable: Pageable): List<T>

    /**
     * Find all rows which match a set of [Condition]s.
     *
     * @param conditions a [Collection] of [Condition]s to pass to the database
     * @param order information used to order the database query
     * @param limit information used to constrain the size and offset of the query
     * @return a [List] of all returned rows, mapped to [T]
     */
    fun findAll(conditions: Collection<Condition>,
                order: Collection<OrderField<*>>,
                limit: Limit): List<T>

    /**
     * Find a single row for a [Record] [T] identified by its [id]
     *
     * @param id The id of the [Record] to fetch
     * @return the matching row, or [Optional.EMPTY] if no row was found
     */
    fun findOne(id: ID): Optional<T>

}