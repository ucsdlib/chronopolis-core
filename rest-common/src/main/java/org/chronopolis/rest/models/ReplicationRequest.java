package org.chronopolis.rest.models;

/**
 * Request for when a Node wants to Replicate content.
 *
 * Created by shake on 11/19/14.
 */
public class ReplicationRequest {

    private Long bagId;
    private Long nodeId;

    public Long getBagId() {
        return bagId;
    }

    public void setBagId(final Long bagId) {
        this.bagId = bagId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public ReplicationRequest setNodeId(Long nodeId) {
        this.nodeId = nodeId;
        return this;
    }

    @Override
    public String toString() {
        return "ReplicationRequest{" +
                "bagId=" + bagId +
                ", nodeId=" + nodeId +
                '}';
    }
}
