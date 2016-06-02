package org.chronopolis.rest.models;

import org.chronopolis.rest.entities.Replication;

/**
 * Different statuses of a {@link Replication}
 *
 * Is there anything wrong with just implementing the clientStatus and failureStatus here?
 *
 * Created by shake on 11/5/14.
 */
public enum ReplicationStatus {

    // Flow enums
    PENDING,
    STARTED,
    TRANSFERRED,
    SUCCESS,
    ACE_REGISTERED,
    ACE_TOKEN_LOADED,
    ACE_AUDITING,

    // Failure enums
    FAILURE_ACE_AUDIT,
    FAILURE_TOKEN_STORE,
    FAILURE_TAG_MANIFEST,
    FAILURE;

    public boolean isFailure() {
        return this == FAILURE
                || this == FAILURE_TAG_MANIFEST
                || this == FAILURE_TOKEN_STORE
                || this == FAILURE_ACE_AUDIT;
    }

    public boolean isClientStatus() {
        return this == STARTED
                // || this == TRANSFERRED
                || this == SUCCESS
                || this == FAILURE
                || this == ACE_AUDITING
                || this == ACE_TOKEN_LOADED
                || this == ACE_REGISTERED;
    }

    // TODO: Can use the isClientStatus() to shorten this
    public boolean isOngoing() {
        return !(this.isFailure() || this == SUCCESS);
    }

}
