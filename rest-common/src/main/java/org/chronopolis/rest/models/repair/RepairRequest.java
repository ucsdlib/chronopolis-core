package org.chronopolis.rest.models.repair;

import java.util.List;

/**
 * Request to repair certain files at a chronopolis node
 *
 * Created by shake on 11/10/16.
 */
public class RepairRequest {

    String depositor;
    String collection;
    List<String> files;

    public String getDepositor() {
        return depositor;
    }

    public RepairRequest setDepositor(String depositor) {
        this.depositor = depositor;
        return this;
    }

    public String getCollection() {
        return collection;
    }

    public RepairRequest setCollection(String collection) {
        this.collection = collection;
        return this;
    }

    public List<String> getFiles() {
        return files;
    }

    public RepairRequest setFiles(List<String> files) {
        this.files = files;
        return this;
    }
}
