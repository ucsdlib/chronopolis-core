package org.chronopolis.ingest.models;

import com.google.common.collect.LinkedListMultimap;

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
        this.page = page;
        parameters.put("page", page.toString());
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

    public LinkedListMultimap<String, String> getParameters() {
        return parameters;
    }
}
