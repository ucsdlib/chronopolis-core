package org.chronopolis.rest.entities.storage;

import org.chronopolis.rest.entities.UpdatableEntity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import java.util.HashSet;
import java.util.Set;

/**
 * Generic storage representation for a bag or tokens or w.e
 *
 * Created by shake on 7/10/17.
 */
@Entity
public class Storage extends UpdatableEntity {

    @ManyToOne
    private StorageRegion region;

    @OneToMany(mappedBy = "storage",
               fetch = FetchType.EAGER,
               cascade = CascadeType.ALL)
    private Set<Fixity> fixities;

    private long size;
    private long totalFiles;
    private String path;
    private boolean active;

    public Storage() {
        // jpa yay
        this.fixities = new HashSet<>();
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

    public Set<Fixity> getFixities() {
        return fixities;
    }

    public Storage addFixity(Fixity fixity) {
        if (fixities == null) {
            fixities = new HashSet<>();
        }

        fixities.add(fixity);
        return this;
    }

    public Storage setFixities(Set<Fixity> fixities) {
        this.fixities = fixities;
        return this;
    }
}
