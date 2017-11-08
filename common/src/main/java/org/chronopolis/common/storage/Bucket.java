package org.chronopolis.common.storage;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import org.chronopolis.common.transfer.RSyncTransfer;

import java.nio.file.Path;
import java.util.Optional;

/**
 * Container for operations over a storage area. Should be able to reject any operation which
 * it cannot support (either via protocol, size limitation, etc). In addition, certain utility
 * methods exist to help general options for files which exist within a Bucket.
 *
 * @author shake
 */
public interface Bucket {

    boolean allocate(StorageOperation operation);
    boolean contains(StorageOperation operation);
    boolean writeable(StorageOperation operation);

    /**
     * Create a FileTransfer which can be used to replicate content into this Bucket
     *
     * @param operation the operation containing information about the Transfer
     * @return the FileTransfer, or an empty optional if no transfer could be created
     */
    Optional<RSyncTransfer> transfer(StorageOperation operation);

    /**
     * Retrieve the hash for a file for a given StorageOperation
     *
     * @param operation the operation which the file belongs to
     * @param file the relative path of the file
     * @return the sha256 hash of the file
     */
    Optional<HashCode> hash(StorageOperation operation, Path file);

    /**
     * Retrieve the input stream of a file for a given StorageOperation
     *
     * @param operation the operation which the file belongs to
     * @param file the relative path of the file
     * @return the OutputStream of the file
     */
    Optional<ByteSource> stream(StorageOperation operation, Path file);

    // just in case we need these ops
    void free(StorageOperation operation);
    void refresh();

}
