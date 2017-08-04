package org.chronopolis.rest.models.storage;

import java.util.HashSet;
import java.util.Set;

/**
 * Model for our Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class StagingStorageModel {

    // private boolean cleaning:
    //   : active == true  && cleaning == false -> staged
    //   : active == true  && cleaning == true  -> invalid
    //   : active == false && cleaning == true  -> removing from stage
    //   : active == false && cleaning == false -> not staged
    //   : how to represent that it is staging? should we have a staging boolean? does it matter?
    // private boolean cleaned
    private boolean active;
    private long size;
    private long region;
    private long totalFiles;
    private String path;

    private Set<Fixity> fixities = new HashSet<>();

    public boolean isActive() {
        return active;
    }

    public StagingStorageModel setActive(boolean active) {
        this.active = active;
        return this;
    }

    public long getSize() {
        return size;
    }

    public StagingStorageModel setSize(long size) {
        this.size = size;
        return this;
    }

    public long getRegion() {
        return region;
    }

    public StagingStorageModel setRegion(long region) {
        this.region = region;
        return this;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public StagingStorageModel setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public String getPath() {
        return path;
    }

    public StagingStorageModel setPath(String path) {
        this.path = path;
        return this;
    }

    public Set<Fixity> getFixities() {
        return fixities;
    }

    public StagingStorageModel addFixity(Fixity fixity) {
        fixities.add(fixity);
        return this;
    }

    public StagingStorageModel setFixities(Set<Fixity> fixities) {
        this.fixities = fixities;
        return this;
    }
}
