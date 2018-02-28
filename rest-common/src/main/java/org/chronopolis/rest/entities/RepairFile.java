package org.chronopolis.rest.entities;

import javax.persistence.Entity;
import javax.persistence.ManyToOne;

/**
 * Join table to keep track of files in a repair
 *
 * Created by shake on 11/10/16.
 */
@Entity
public class RepairFile extends PersistableEntity {

    @ManyToOne
    private Repair repair;

    private String path;

    public RepairFile() {
    }

    public Repair getRepair() {
        return repair;
    }

    public RepairFile setRepair(Repair repair) {
        this.repair = repair;
        return this;
    }

    public String getPath() {
        return path;
    }

    public RepairFile setPath(String path) {
        this.path = path;
        return this;
    }

    public String toString() {
        return path;
    }
}
