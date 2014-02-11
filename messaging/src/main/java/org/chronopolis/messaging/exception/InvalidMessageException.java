package org.chronopolis.messaging.exception;

/**
 * Created by shake on 2/4/14.
 */
public class InvalidMessageException extends RuntimeException {
    public InvalidMessageException(String msg, Throwable t) {
        super(msg, t);
    }
}
