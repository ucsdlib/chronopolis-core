package org.chronopolis.ingest.models;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryModifiers;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Hold common paging attributes
 *
 * todo: mutable page size
 *
 * Created by shake on 6/15/17.
 */
public abstract class Paged {

    private static final long DEFAULT_PAGE_SIZE = 25;

    private String dir;
    private Integer page = 0;
    private String orderBy = "id";

    private final LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

    public String getDir() {
        return dir;
    }

    public Paged setDir(String dir) {
        this.dir = dir;
        parameters.put("dir", dir);
        return this;
    }

    public Integer getPage() {
        return page;
    }

    public Paged setPage(Integer page) {
        // We don't add the page parameter to our multimap bc it is appended in the page fragment
        this.page = page;
        return this;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public Paged setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        parameters.put("orderBy", orderBy);
        return this;
    }

    /**
     * Retrieve a QueryModifier (limit and offset) in order to restrict the size of the result
     * set and provide pagination
     *
     * @return the QueryModifier with the limit and offset applied
     */
    public QueryModifiers getRestriction() {
        long offset = page * DEFAULT_PAGE_SIZE;
        return new QueryModifiers(DEFAULT_PAGE_SIZE, offset);
    }

    /**
     * Retrieve the direction which a ResultSet should be sorted
     *
     * If no direction is specified, default to DESC
     *
     * @return the Order representing the Direction
     */
    public Order getDirection() {
        return (dir == null) ? Order.DESC : Order.valueOf(dir);
    }

    /**
     * Create a PageRequest used to sort a result set and limit its size in order to provide
     * pagination
     *
     * @return the page request
     * @deprecated being phased out in favor of handling pagination through QueryDSL
     */
    @Deprecated
    public PageRequest createPageRequest() {
        Sort.Direction direction = (dir == null)
                ? Sort.Direction.ASC
                : Sort.Direction.fromString(dir);
        Sort s = new Sort(direction, orderBy);
        return new PageRequest(page, (int) DEFAULT_PAGE_SIZE, s);
    }

    /**
     * Retrieve the QueryParameters passed in to the Filter
     *
     * @return a Multimap containing all parameters passed to this Filter
     */
    public Multimap<String, String> getParameters() {
        return Multimaps.filterValues(parameters, (value) -> (value != null && !value.isEmpty()));
    }

    /**
     * Retrieve the BooleanBuilder representing the conditionals built up for use in a
     * DB query.
     *
     * @return the BooleanBuilder
     */
    public abstract BooleanBuilder getQuery();

    /**
     * Retrieve the OrderSpecifier used for sorting a result set in a DB Query
     *
     * @return the OrderSpecifier
     */
    public abstract OrderSpecifier getOrderSpecifier();
}
