package org.chronopolis.ingest.models;

import org.chronopolis.rest.models.BagStatus;

/**
 * Model for updating bags
 *
 * May be expanded as we allow more parts of a bag to be mutable
 *
 * Created by shake on 4/28/15.
 */
public class BagUpdate {

    private BagStatus status;

    public BagUpdate() {
    }

    public BagStatus getStatus() {
        return status;
    }

    public BagUpdate setStatus(BagStatus status) {
        this.status = status;
        return this;
    }
}
