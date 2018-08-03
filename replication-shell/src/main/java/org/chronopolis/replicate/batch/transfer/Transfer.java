package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.rest.kot.models.Bag;
import org.chronopolis.rest.kot.models.Replication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Callback;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Common code between our bag and token transfers
 * <p>
 * Created by shake on 2/17/17.
 */
public interface Transfer {

    /**
     * Update (the Ingest API) with a given hash value
     *
     * @param hash the hash value to update the Ingest API with
     * @return the UpdateCallback for the connection to the Ingest API
     */
    Callback<Replication> update(HashCode hash);

    /**
     * Initiate a FileTransfer and log the results
     *
     * @param transfer the transfer to initiate
     * @param id       the id of the operation
     * @return the top level path of the transfer
     */
    default Optional<Path> transfer(FileTransfer transfer, String id) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        Optional<Path> result = Optional.empty();
        try {
            // so interestingly enough... we don't rely on the bag path anymore...
            // but use it as a way to determine if the transfer succeeded I guess
            result = Optional.ofNullable(transfer.get());
            log(id, transfer.getOutput());
        } catch (FileTransferException e) {
            log(id, transfer.getErrors());
            log.error("[{}] File transfer exception", id, e);
            fail(e);
        }
        return result;
    }

    /**
     * Hash a file for a given bag
     * <p>
     * The exception thrown is a RuntimeException so that this can be used
     * in a chain of CompletableFutures without having a checked exception
     *
     * @param bag  The bag we are operating on
     * @param path The path of the file to hash
     * @throws RuntimeException if there's an error hashing
     */
    default void hash(Bag bag, Path path) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        HashFunction hashFunction = Hashing.sha256();
        HashCode hash;
        try {
            // Check to make sure the download was successful
            if (!path.toFile().exists()) {
                throw new IOException("File "
                        + path.toString()
                        + " does does not exist");
            }

            hash = Files.asByteSource(path.toFile()).hash(hashFunction);
            update(hash);
        } catch (IOException e) {
            log.error("{} Error hashing file", bag.getName(), e);
            fail(e);
        }
    }

    /**
     * Fail a replication by throwing a RuntimeException
     *
     * @param e The checked exception we are swallowing
     * @throws RuntimeException unchecked version of e
     */
    default void fail(Exception e) {
        throw new RuntimeException(e);
    }

    /**
     * Log information for a bag, typically from the rsync output
     *
     * @param bag    The bag to operate on
     * @param stream The InputStream to read from
     */
    default void log(Bag bag, InputStream stream) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try (Stream<String> lines = reader.lines()) {
            lines.forEach(line -> log.info("{} {}", bag.getName(), line));
        }
    }

    /**
     * Same as the other log
     *
     * @param name
     * @param stream
     */
    default void log(String name, InputStream stream) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try (Stream<String> lines = reader.lines()) {
            lines.forEach(line -> log.info("{} {}", name, line));
        }
    }
}
