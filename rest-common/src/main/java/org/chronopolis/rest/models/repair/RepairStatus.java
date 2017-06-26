package org.chronopolis.rest.models.repair;

import com.google.common.collect.ImmutableListMultimap;

/**
 * Enumeration of the various statuses our repairs can have
 *
 * Created by shake on 11/10/16.
 */
public enum RepairStatus {
    REQUESTED, STAGING, READY, TRANSFERRED, REPAIRED, FAILED;

    public static ImmutableListMultimap<String, RepairStatus> statusByGroup() {
        return new ImmutableListMultimap.Builder<String, RepairStatus>()
                .put("Pending", RepairStatus.REQUESTED)
                .put("Active", RepairStatus.STAGING)
                .put("Active", RepairStatus.READY)
                .put("Active", RepairStatus.TRANSFERRED)
                .put("Success", RepairStatus.REPAIRED)
                .put("Failure", RepairStatus.FAILED)
                .build();
    }
}
