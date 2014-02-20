package org.chronopolis.common.exception;

/**
 * Created by shake on 2/20/14.
 */
public class FileTransferException extends RuntimeException{
    public FileTransferException(String msg, Throwable t) {
        super(msg, t);
    }

    public FileTransferException(String msg) {
        super(msg);
    }
}
