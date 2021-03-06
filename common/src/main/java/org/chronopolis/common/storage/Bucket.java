package org.chronopolis.common.storage;

import com.google.common.hash.HashCode;
import com.google.common.io.ByteSource;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.transfer.FileTransfer;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Container for operations over a storage area. Should be able to reject any operation which
 * it cannot support (either via protocol, size limitation, etc). In addition, certain utility
 * methods exist to help general options for files which exist within a Bucket.
 *
 * @author shake
 */
public interface Bucket {

    /**
     * Try to add a StorageOperation to a Bucket, failing if there is not enough space
     * in the bucket
     *
     * @param operation the operation to add
     * @return true if there is room; false otherwise
     */
    boolean allocate(StorageOperation operation);

    /**
     * Check if a Bucket contains a StorageOperation:
     * if it is pending or if it already exists in the Bucket
     *
     * @param operation the operation to check
     * @return true if the operation exists; false otherwise
     */
    boolean contains(StorageOperation operation);

    /**
     * Check if a Bucket contains a given file for a StorageOperation
     *
     * @param operation the operation to check
     * @param file      the file in the operation
     * @return true if the file exists for the operation; false otherwise
     */
    boolean contains(StorageOperation operation, Path file);

    /**
     * Check if a Bucket has enough usable storage to write the total size of a StorageOperation
     * i.e. operation.size < bucket.usable
     *
     * @param operation the operation to check
     * @return true if space is available; false otherwise
     */
    boolean writeable(StorageOperation operation);

    /**
     * Create directories up to the operation's path
     *
     * @param operation the operation to create the directories for
     * @return the path to the operation's root directory, if available
     */
    Optional<Path> mkdir(StorageOperation operation);

    /**
     * Create a FileTransfer which can be used to replicate content into this Bucket
     *
     * @param operation the operation containing information about the Transfer
     * @return the FileTransfer, or an empty optional if no transfer could be created
     */
    Optional<FileTransfer> transfer(StorageOperation operation);

    /**
     * Create a FileTransfer with additional arguments which can be used to replicate content
     * into this Bucket
     *
     * @param operation the operation containing information about the Transfer
     * @param arguments the arguments to pass along to the Transfer
     * @return the FileTransfer, or an empty optional if no transfer could be created
     */
    Optional<FileTransfer> transfer(StorageOperation operation, List<String> arguments);


    /**
     * Retrieve the hash for a file for a given StorageOperation
     *
     * @param operation the operation which the file belongs to
     * @param file      the relative path of the file
     * @return the sha256 hash of the file
     */
    Optional<HashCode> hash(StorageOperation operation, Path file);

    /**
     * Retrieve the input stream of a file for a given StorageOperation
     *
     * @param operation the operation which the file belongs to
     * @param file      the relative path of the file
     * @return the OutputStream of the file
     */
    Optional<ByteSource> stream(StorageOperation operation, Path file);

    /**
     * Update the ACE Collection.Builder with storage information for this
     * bucket
     *
     * @param operation  the operation to create the ACE storage for
     * @param collection the Builder for creating the Ace Collection Gson model
     * @return the updated {@link GsonCollection.Builder}
     */
    GsonCollection.Builder fillAceStorage(StorageOperation operation,
                                          GsonCollection.Builder collection);

    /**
     * What does it mean to free an operation in our context? Remove it from disk?
     * Reclaim its allocated space?
     * <p>
     * For now it might be best only to use this when a replication cannot be recovered and needs to
     * be removed from a Bucket
     *
     * @param operation the operation to free
     */
    void free(StorageOperation operation);

}
