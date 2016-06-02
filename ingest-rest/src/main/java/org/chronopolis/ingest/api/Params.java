package org.chronopolis.ingest.api;

/**
 * Basic query parameter names for us to use
 *
 * Created by shake on 1/12/15.
 */
public class Params {
    // Query
    public static final String NAME = "name";
    public static final String NODE = "node";
    public static final String DEPOSITOR = "depositor";
    public static final String PAGE  = "page";
    public static final String PAGE_SIZE = "page_size";
    public static final String STATUS = "status";

    // Sorting
    public static final String SORT_DIRECTION = "direction";
    public static final String SORT_BY_SIZE = "by_size";
    public static final String SORT_BY_TOTAL_FILES = "by_total_files";

    // Sorting property names
    public static final String SORT_ID = "id";
    public static final String SORT_SIZE = "size";
    public static final String SORT_TOTAL_FILES = "totalFiles";

}
