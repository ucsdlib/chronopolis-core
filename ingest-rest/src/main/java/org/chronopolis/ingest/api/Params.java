package org.chronopolis.ingest.api;

/**
 * Basic query parameter names for us to use
 *
 * Created by shake on 1/12/15.
 */
public class Params {
    // Query
    public static final String TO = "to";
    public static final String NAME = "name";
    public static final String NODE = "node";
    public static final String TYPE = "type";
    public static final String FROM = "from";
    public static final String ACTIVE = "active";
    public static final String REGION = "region";
    public static final String CREATOR = "creator";
    public static final String DEPOSITOR = "depositor";
    public static final String REQUESTER = "requester";
    public static final String PAGE  = "page";
    public static final String PAGE_SIZE = "page_size";
    public static final String STATUS = "status";
    public static final String CLEANED = "cleaned";
    public static final String CHECKSUM = "checksum";
    public static final String REPLACED = "replaced";
    public static final String VALIDATED = "validated";
    public static final String CAPACITY_LT = "capacity_lt";
    public static final String CAPACITY_GT = "capacity_gt";
    public static final String DATA_TYPE = "dataType";
    public static final String STORAGE_TYPE = "storageType";

    // Sorting
    public static final String SORT_DIRECTION = "direction";
    public static final String SORT_BY_SIZE = "by_size";
    public static final String SORT_BY_TOTAL_FILES = "by_total_files";

    // Sorting property names
    public static final String SORT_ID = "id";
    public static final String SORT_SIZE = "size";
    public static final String SORT_TOTAL_FILES = "totalFiles";

    // Temporal stuff
    public static final String CREATED_AFTER = "created_after";
    public static final String UPDATED_AFTER = "updated_after";
    public static final String CREATED_BEFORE = "created_before";
    public static final String UPDATED_BEFORE = "updated_before";
}
