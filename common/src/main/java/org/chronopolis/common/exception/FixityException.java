package org.chronopolis.common.exception;

/**
 * Created by shake on 9/8/14.
 */
public class FixityException extends Exception {

    public FixityException(String msg) {
        super(msg);
    }

    public FixityException(String msg, Throwable t) {
        super(msg, t);
    }
}
