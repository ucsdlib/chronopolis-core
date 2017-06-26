package org.chronopolis.rest.entities;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Join table to keep track of files in a repair
 *
 * Created by shake on 11/10/16.
 */
@Entity
public class RepairFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne
    Repair repair;

    String path;

    public RepairFile() {
    }

    public Long getId() {
        return id;
    }

    public RepairFile setId(Long id) {
        this.id = id;
        return this;
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
