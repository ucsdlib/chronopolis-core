package org.chronopolis.rest.models.repair;

import java.util.Optional;
import java.util.Set;

/**
 * Request to repair certain files at a chronopolis node
 *
 * Created by shake on 11/10/16.
 */
public class RepairRequest {

    String to;
    String depositor;
    String collection;
    Set<String> files;

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

    public Set<String> getFiles() {
        return files;
    }

    public RepairRequest setFiles(Set<String> files) {
        this.files = files;
        return this;
    }

    public Optional<String> getTo() {
        return Optional.ofNullable(to);
    }

    public RepairRequest setTo(String to) {
        this.to = to;
        return this;
    }
}
