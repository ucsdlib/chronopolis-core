package org.chronopolis.rest.models;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;

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

    public static ImmutableCollection<BagStatus> processingStates() {
        return ImmutableSet.of(DEPOSITED, INITIALIZED, TOKENIZED, REPLICATING);
    }

    public static ImmutableCollection<BagStatus> preservedStates() {
        return ImmutableSet.of(PRESERVED);
    }

    public static ImmutableCollection<BagStatus> inactiveStates() {
        return ImmutableSet.of(DEPRECATED, DELETED, ERROR);
    }

    public static ImmutableListMultimap<String, BagStatus> statusByGroup() {
        return new ImmutableListMultimap.Builder<String, BagStatus>()
                .put("Processing", BagStatus.DEPOSITED)
                .put("Processing", BagStatus.INITIALIZED)
                .put("Processing", BagStatus.TOKENIZED)
                .put("Processing", BagStatus.REPLICATING)
                .put("Preserved", BagStatus.PRESERVED)
                .put("Inactive", BagStatus.DEPRECATED)
                .put("Inactive", BagStatus.DELETED)
                .put("Inactive", BagStatus.ERROR)
                .build();
    }
}
