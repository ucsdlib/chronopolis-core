package org.chronopolis.rest.models.page

/**
 * A wrapper around a Page from spring to avoid pulling in spring-data dependencies
 *
 * Since we don't make use of most of the Pageable/Slice interfaces, we can ignore them
 *
 * @author shake
 */
data class SpringPage<out T>(val content: List<T>,
                             val last: Boolean,
                             val first: Boolean,
                             val totalPages: Int,
                             val totalElements: Long,
                             val sort: List<Sort>,
                             val numberOfElements: Int,
                             val size: Int,
                             val number: Int) : Iterable<T> {
    override fun iterator(): Iterator<T> {
        return content.iterator()
    }
}

fun <T> List<T>.wrap(): SpringPage<T> {
    return SpringPage(this, true, true, 1, this.size.toLong(), listOf(), this.size, this.size, 1)
}

data class Sort(val direction: String,
                val property: String,
                val ignoreCase: Boolean,
                val nullHandling: String,
                val ascending: Boolean,
                val descending: Boolean)