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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RStatusUpdate that = (RStatusUpdate) o;

        return status == that.status;

    }

    @Override
    public int hashCode() {
        return status != null ? status.hashCode() : 0;
    }
}
