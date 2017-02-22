package org.chronopolis.rest.models;

/**
 * Status types for our bags. Subject to change.
 *
 * TODO: As these grow it might be a good time to look
 *       for ways to trim them down and see what is no
 *       longer necessary.
 *
 * Created by shake on 11/20/14.
 */
public enum BagStatus {
    @Deprecated
    STAGED("STAGED"),
    @Deprecated
    REPLICATED("REPLICATED"),

    DEPOSITED("DEPOSITED"),
    INITIALIZED("INITIALIZED"),
    TOKENIZED("TOKENIZED"),
    REPLICATING("REPLICATING"),
    PRESERVED("PRESERVED"),
    DEPRECATED("DEPRECATED"),
    DELETED("DELETED"),
    ERROR("ERROR");

    private String value;

    BagStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
