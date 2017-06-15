package org.chronopolis.ingest.models;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Hold common paging attributes
 *
 * Created by shake on 6/15/17.
 */
public class Paged {

    private String dir;
    private Integer page = 0;
    private String orderBy = "id";

    private LinkedListMultimap<String, String> parameters = LinkedListMultimap.create();

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

    public Multimap<String, String> getParameters() {
        return Multimaps.filterValues(parameters, (value) -> (value != null && !value.isEmpty()));
    }
}
