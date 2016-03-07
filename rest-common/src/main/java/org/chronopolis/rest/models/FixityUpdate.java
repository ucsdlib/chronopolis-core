package org.chronopolis.rest.models;

/**
 *
 * Created by shake on 12/1/15.
 */
public class FixityUpdate {

    private String fixity;

    public FixityUpdate() {
    }

    public FixityUpdate(String fixity) {
        this.fixity = fixity;
    }

    public String getFixity() {
        return fixity;
    }

    public FixityUpdate setFixity(String fixity) {
        this.fixity = fixity;
        return this;
    }
}
