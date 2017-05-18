package org.chronopolis.rest.models.repair;

/**
 * Enumeration of the various statuses our repairs can have
 *
 * Created by shake on 11/10/16.
 */
public enum RepairStatus {
    REQUESTED, STAGING, READY, TRANSFERRED, REPAIRED, FAILED
}
