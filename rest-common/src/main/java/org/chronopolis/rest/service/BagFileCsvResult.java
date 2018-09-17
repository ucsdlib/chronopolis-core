package org.chronopolis.rest.service;

import java.nio.file.Path;
import java.util.Optional;

/**
 * The result of attempting to create a CSV of BagFiles
 *
 * @author shake
 */
public class BagFileCsvResult {

    private Path csv;
    private Throwable error;

    public BagFileCsvResult(Path csv) {
        this.csv = csv;
        this.error = null;
    }

    public BagFileCsvResult(Throwable error) {
        this.csv = null;
        this.error = error;
    }

    public Optional<Path> getCsv() {
        return Optional.ofNullable(csv);
    }

    public BagFileCsvResult setCsv(Path csv) {
        this.csv = csv;
        return this;
    }

    public Throwable getError() {
        return error;
    }

    public BagFileCsvResult setError(Throwable error) {
        this.error = error;
        return this;
    }

    public boolean isSuccess() {
        return csv != null;
    }
}
