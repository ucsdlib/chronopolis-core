package org.chronopolis.ingest.models;

import org.chronopolis.rest.models.BagStatus;

/**
 * Created by shake on 4/28/15.
 */
public class BagUpdate {

    BagStatus status;

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
