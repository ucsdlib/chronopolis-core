package org.chronopolis.replicate.batch.rsync;

/**
 * Just an idea... used to define the workflow for an rsync
 *
 * e.g. default -> execute a single rsync per collection
 *      chunked -> execute multiple rsyncs per collection using '--from-files'
 *
 * @author shake
 */
public enum Profile {
    SINGLE, CHUNKED
}
