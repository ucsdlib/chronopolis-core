package org.chronopolis.rest.models.storage;

import java.util.HashSet;
import java.util.Set;

/**
 * Model for our Storage entity
 *
 * Created by shake on 7/11/17.
 */
public class Storage {

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

    public Storage setActive(boolean active) {
        this.active = active;
        return this;
    }

    public long getSize() {
        return size;
    }

    public Storage setSize(long size) {
        this.size = size;
        return this;
    }

    public long getRegion() {
        return region;
    }

    public Storage setRegion(long region) {
        this.region = region;
        return this;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public Storage setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }

    public String getPath() {
        return path;
    }

    public Storage setPath(String path) {
        this.path = path;
        return this;
    }

    public Set<Fixity> getFixities() {
        return fixities;
    }

    public Storage addFixity(Fixity fixity) {
        fixities.add(fixity);
        return this;
    }

    public Storage setFixities(Set<Fixity> fixities) {
        this.fixities = fixities;
        return this;
    }
}
