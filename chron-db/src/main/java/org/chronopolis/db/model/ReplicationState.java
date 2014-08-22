package org.chronopolis.db.model;

/**
 *
 *
 * Created by shake on 6/12/14.
 */
public enum ReplicationState {
    // TODO: Do we want to have states for token store download/bag downloaded?

    INIT,
    REPLICATING,
    RETRY,
    FINISHED,
    FAILED
}
