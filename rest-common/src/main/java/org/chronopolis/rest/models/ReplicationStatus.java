package org.chronopolis.rest.models;

/**
 * Different statuses of a {@link Replication}
 *
 * Created by shake on 11/5/14.
 */
public enum ReplicationStatus {

    PENDING,
    STARTED,
    TRANSFERRED,
    SUCCESS,
    FAILURE_TOKEN_STORE,
    FAILURE_TAG_MANIFEST,
    FAILURE

}
