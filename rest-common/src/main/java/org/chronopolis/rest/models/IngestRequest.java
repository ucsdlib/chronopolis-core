package org.chronopolis.rest.models;

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Request for creating a new Bag
 *
 * Created by shake on 11/6/14.
 */
public class IngestRequest {

    String name;
    String location;
    String depositor;
    int requiredReplications;
    List<String> replicatingNodes;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public int getRequiredReplications() {
        return requiredReplications;
    }

    public IngestRequest setRequiredReplications(int requiredReplications) {
        this.requiredReplications = requiredReplications;
        return this;
    }

    public List<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public IngestRequest setReplicatingNodes(List<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("location", location)
                .add("depositor", depositor)
                .add("requiredReplications", requiredReplications)
                .add("replicatingNodes", replicatingNodes)
                .toString();
    }
}
