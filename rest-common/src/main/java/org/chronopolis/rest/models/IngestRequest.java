package org.chronopolis.rest.models;

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Request for creating a new Bag
 *
 * Created by shake on 11/6/14.
 */
public class IngestRequest {

    private String name;
    private Long size;
    private Long totalFiles;
    private Long storageRegion;
    private String location;
    private String depositor;
    private int requiredReplications;
    private List<String> replicatingNodes;

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

    public Long getStorageRegion() {
        return storageRegion;
    }

    public IngestRequest setStorageRegion(Long storageRegion) {
        this.storageRegion = storageRegion;
        return this;
    }

    public Long getSize() {
        return size;
    }

    public IngestRequest setSize(Long size) {
        this.size = size;
        return this;
    }

    public Long getTotalFiles() {
        return totalFiles;
    }

    public IngestRequest setTotalFiles(Long totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("depositor", depositor)
                .add("location", location)
                .add("size", size)
                .add("totalFiles", totalFiles)
                .add("storageRegion", storageRegion)
                .add("requiredReplications", requiredReplications)
                .add("replicatingNodes", replicatingNodes)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IngestRequest that = (IngestRequest) o;

        if (requiredReplications != that.requiredReplications) return false;
        if (name != null ? !name.equals(that.name) : that.name != null) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;
        if (depositor != null ? !depositor.equals(that.depositor) : that.depositor != null) return false;
        return replicatingNodes != null ? replicatingNodes.equals(that.replicatingNodes) : that.replicatingNodes == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (location != null ? location.hashCode() : 0);
        result = 31 * result + (depositor != null ? depositor.hashCode() : 0);
        result = 31 * result + requiredReplications;
        result = 31 * result + (replicatingNodes != null ? replicatingNodes.hashCode() : 0);
        return result;
    }
}
