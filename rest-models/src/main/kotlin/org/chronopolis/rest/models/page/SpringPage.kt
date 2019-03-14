package org.chronopolis.rest.models.page

/**
 * A wrapper around a Page from spring to avoid pulling in spring-data dependencies
 *
 * Since we don't make use of most of the Pageable/Slice interfaces, we can ignore them
 *
 * @author shake
 */
data class SpringPage<out T>(val content: List<T>,
                             val pageable: Pageable,
                             val last: Boolean,
                             val totalPages: Int,
                             val totalElements: Long,
                             val first: Boolean,
                             val sort: Sort,
                             val numberOfElements: Int,
                             val size: Int,
                             val number: Int,
                             val empty: Boolean) : Iterable<T> {
    override fun iterator(): Iterator<T> {
        return content.iterator()
    }
}

fun <T> List<T>.wrap(): SpringPage<T> {
    val sort = Sort(true, false, false)
    val page = Pageable(sort, this.size, 1, 0, false, true)
    return SpringPage(this, page, true, 1, 1L, true, sort, this.size, this.size, 1, this.isEmpty())
}

data class Pageable(val sort: Sort,
                    val pageSize: Int,
                    val pageNumber: Int,
                    val offset: Int,
                    val unpaged: Boolean,
                    val paged: Boolean)

data class Sort(val sorted: Boolean,
                val unsorted: Boolean,
                val empty: Boolean)