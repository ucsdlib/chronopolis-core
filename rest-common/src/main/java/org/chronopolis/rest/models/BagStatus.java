package org.chronopolis.rest.models;

/**
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
