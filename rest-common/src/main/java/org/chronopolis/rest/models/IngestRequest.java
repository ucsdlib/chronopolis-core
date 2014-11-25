package org.chronopolis.rest.models;

/**
 * Created by shake on 11/6/14.
 */
public class IngestRequest {

    String name;
    String fileName;
    String depositor;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
