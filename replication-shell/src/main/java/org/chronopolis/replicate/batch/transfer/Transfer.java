package org.chronopolis.replicate.batch.transfer;

import org.chronopolis.common.exception.FileTransferException;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.rest.models.Bag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
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
     * @param name   The name of the Bag being operated on
     * @param stream The InputStream to read from
     */
    default void log(String name, InputStream stream) {
        Logger log = LoggerFactory.getLogger("rsync-log");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try (Stream<String> lines = reader.lines()) {
            lines.forEach(line -> log.info("{} {}", name, line));
        }
    }
}
