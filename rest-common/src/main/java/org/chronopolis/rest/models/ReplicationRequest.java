package org.chronopolis.rest.models;

/**
 * Request for when a Node wants to Replicate content.
 *
 * Created by shake on 11/19/14.
 */
public class ReplicationRequest {

    private Long bagID;

    public Long getBagId() {
        return bagID;
    }

    public void setBagId(final Long bagID) {
        this.bagID = bagID;
    }
}
