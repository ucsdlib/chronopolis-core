package org.chronopolis.common.exception;

/**
 * Created by shake on 2/20/14.
 */
public class FileTransferException extends Exception {

    public FileTransferException(final String msg, final Throwable t) {
        super(msg, t);
    }

    public FileTransferException(final String msg) {
        super(msg);
    }
}
