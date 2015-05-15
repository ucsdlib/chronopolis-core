package org.chronopolis.common.dpn;

/**
 * Needed because the django serialization is broken
 *
 * Created by shake on 5/14/15.
 */
public class Fixity {

    private String sha256;

    public Fixity() {
    }

    public Fixity(String sha256) {
        this.sha256 = sha256;
    }

    public String getSha256() {
        return sha256;
    }

    public Fixity setSha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }
}
