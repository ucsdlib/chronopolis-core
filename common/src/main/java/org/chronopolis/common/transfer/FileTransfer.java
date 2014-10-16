package org.chronopolis.common.transfer;

import org.chronopolis.common.exception.FileTransferException;

import java.nio.file.Path;

/**
 * Created by shake on 2/17/14.
 */
public interface FileTransfer {

    /**
     * Retrieve a file from a remote uri
     *
     * @param uri The location of the file
     * @param localStorage The directory to pull the file into
     * @return The path of the file on disk
     * @throws FileTransferException if the transfer fails
     */
    Path getFile(String uri, Path localStorage) throws FileTransferException;

    /**
     * Put a local file to a remote uri
     *
     * @param localFile
     * @param uri
     * @throws FileTransferException
     */
    void put(Path localFile, String uri) throws FileTransferException;
}
