package org.chronopolis.replicate.batch.transfer;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import org.chronopolis.rest.models.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Common code between our bag and token transfers
 *
 * Created by shake on 2/17/17.
 */
public interface Transfer {

    void update(HashCode hash);

    /**
     * Hash a file for a given bag
     *
     * The exception thrown is a RuntimeException so that this can be used
     * in a chain of CompletableFutures without having a checked exception
     *
     * @param bag The bag we are operating on
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

            hash = Files.hash(path.toFile(), hashFunction);
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
     * @param bag The bag to operate on
     * @param stream The InputStream to read from
     */
    default void log(Bag bag, InputStream stream) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try (Stream<String> lines = reader.lines()) {
            lines.forEach(line -> log.info("{} {}", bag.getName(), line));
        }
    }
}
