package org.chronopolis.common.transfer;

import org.chronopolis.common.exception.FileTransferException;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Interface defining what a FileTransfer should provide in Chronopolis. Possibly implement
 * Supplier in order to provide get (makes exception handling harder, however).
 *
 * Created by shake on 2/17/14.
 */
public interface FileTransfer {

    /**
     * Retrieve a file from a remote uri
     *
     * @param uri          The location of the file
     * @param localStorage The directory to pull the file into
     * @return The path of the file on disk
     * @throws FileTransferException if the transfer fails
     */
    @Deprecated
    Path getFile(String uri, Path localStorage) throws FileTransferException;

    /**
     * Execute this FileTransfer
     *
     * @return The path of the file on disk, or the root directory if the transfer
     *         involves multiple files
     * @throws FileTransferException if the transfer fails
     */
    Path get() throws FileTransferException;

    /**
     * Return the statistics for the given transfer (ie: transfer speed, amount, etc)
     *
     * @return the output information for the transfer, if given
     */
    String getStats();

    InputStream getOutput();
    InputStream getErrors();
}
