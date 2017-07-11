package org.chronopolis.rest.entities.storage;

import org.chronopolis.rest.entities.UpdatableEntity;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Generic storage representation for a bag or tokens or w.e
 *
 * Created by shake on 7/10/17.
 */
@Entity
public class Storage extends UpdatableEntity {

    @ManyToOne
    private StorageRegion region;

    private long size;
    private long totalFiles;
    private String path;
    private String checksum;
    private boolean active;

    public Storage() {
        // jpa yay
    }

    public long getSize() {
        return size;
    }

    public Storage setSize(long size) {
        this.size = size;
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

    public boolean isActive() {
        return active;
    }

    public Storage setActive(boolean active) {
        this.active = active;
        return this;
    }

    public StorageRegion getRegion() {
        return region;
    }

    public Storage setRegion(StorageRegion region) {
        this.region = region;
        return this;
    }
}
