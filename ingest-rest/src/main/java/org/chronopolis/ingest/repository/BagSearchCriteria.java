package org.chronopolis.ingest.repository;

import org.chronopolis.rest.models.BagStatus;

/**
 * Search criteria that map to the query parameters one may pass in when getting bags
 *
 * Created by shake on 5/20/15.
 */
public class BagSearchCriteria {

    private String name;
    private String depositor;
    private BagStatus status;

    public BagSearchCriteria() {
        this.name = "";
        this.depositor = "";
    }

    public BagSearchCriteria withName(String name) {
        if (name == null) {
            name = "";
        }

        this.name = name;
        return this;
    }

    public BagSearchCriteria withDepositor(String depositor) {
        if (depositor == null) {
            depositor = "";
        }

        this.depositor = depositor;
        return this;
    }

    public BagSearchCriteria withStatus(BagStatus status) {
        this.status = status;
        return this;
    }

    public String getName() {
        return name;
    }

    public String getDepositor() {
        return depositor;
    }

    public BagStatus getStatus() {
        return status;
    }
}
