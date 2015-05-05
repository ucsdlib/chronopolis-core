package org.chronopolis.rest.models;

/**
 * Request for creating a new Bag
 *
 * Created by shake on 11/6/14.
 */
public class IngestRequest {

    String name;
    String location;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }
}
