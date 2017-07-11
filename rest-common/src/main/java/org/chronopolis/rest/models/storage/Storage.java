package org.chronopolis.rest.models.storage;

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
    private String checksum;

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

    public String getChecksum() {
        return checksum;
    }

    public Storage setChecksum(String checksum) {
        this.checksum = checksum;
        return this;
    }
}
