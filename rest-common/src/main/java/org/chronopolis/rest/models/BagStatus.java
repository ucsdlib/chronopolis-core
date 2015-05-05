package org.chronopolis.rest.models;

/**
 * Status types for our bags. Subject to change.
 *
 * TODO: Replicating -> Preserved?
 *
 * Created by shake on 11/20/14.
 */
public enum BagStatus {
    STAGED("STAGED"), TOKENIZED("TOKENIZED"), REPLICATING("REPLICATING"), REPLICATED("REPLICATED"), ERROR("ERROR");

    private String value;

    BagStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
