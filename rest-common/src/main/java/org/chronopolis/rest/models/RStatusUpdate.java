package org.chronopolis.rest.models;

/**
 *
 * Created by shake on 3/4/16.
 */
public class RStatusUpdate {

    private ReplicationStatus status;

    public RStatusUpdate() {
    }

    public RStatusUpdate(ReplicationStatus status) {
        this.status = status;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

}
