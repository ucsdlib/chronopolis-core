package org.chronopolis.common.storage;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.common.transfer.FileTransfer;
import org.chronopolis.common.transfer.RSyncTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Bucket for posix-like systems
 *
 * @author shake
 */
public class PosixBucket implements Bucket {

    private final Logger log = LoggerFactory.getLogger(PosixBucket.class);

    private final ImmutableSet<OperationType> supportedOperations = ImmutableSet.of(OperationType.RSYNC);

    private final Posix posix;
    private final Set<StorageOperation> pendingOperations;

    private Long usable;
    private final Long total;

    public PosixBucket(Posix posix) throws IOException {
        this.posix = posix;
        this.pendingOperations = new ConcurrentSkipListSet<>();

        // Grab some values from the FileStore
        // Note that this can fail, and if it does we should throw a
        // BeanCreationException to fail the entire startup
        FileStore store = java.nio.file.Files.getFileStore(Paths.get(posix.getPath()));
        this.total = store.getTotalSpace();
        this.usable = store.getUsableSpace();
    }

    @Override
    public boolean allocate(StorageOperation operation) {
        boolean allocated = false;

        // do we want a lock when checking all of these? it would probably be useful but
        // I guess for now we can just use a single thread for allocation and what not
        if (contains(operation)) {
            allocated = true;
        } else if (writeable(operation) && supported(operation.getType())) {
            // todo: could do one more check for sanity that (usable - size) > 0
            usable -= operation.getSize();
            pendingOperations.add(operation);
            allocated = true;
        }

        return allocated;
    }

    private boolean supported(OperationType type) {
        return supportedOperations.contains(type);
    }

    @Override
    public boolean contains(StorageOperation operation) {
        Path path = Paths.get(posix.getPath()).resolve(operation.getRoot());
        return pendingOperations.contains(operation) || path.toFile().exists();
    }

    @Override
    public boolean contains(StorageOperation operation, Path file) {
        Path path = Paths.get(posix.getPath()).resolve(operation.getRoot()).resolve(file);
        return pendingOperations.contains(operation) || path.toFile().exists();
    }

    @Override
    public boolean writeable(StorageOperation operation) {
        boolean writeable = false;

        // There's still an issue here of multiple operations accessing the
        // various fields concurrently, but we're operating in a ST manner
        // at the moment so syncing/locking can be handled at a later time

        Long size = operation.getSize();
        // not sure if there will be overflow issues on the other side... we're testing with
        // Long.MAX so we should be ok
        Double remainder = (double) (usable - size);

        log.trace("[{}] Total {}; Remainder {} ({} %(UL))", operation.getIdentifier(), total, remainder, remainder / total);
        if (remainder > 0 && remainder / total >= 0.1) {
            writeable = true;
        }
        return writeable;
    }

    @Override
    public Optional<Path> mkdir(StorageOperation operation) {
        Path path = Paths.get(posix.getPath()).resolve(operation.getRoot());

        Optional<Path> root = Optional.empty();
        try {
            java.nio.file.Files.createDirectories(path);
            root = Optional.of(path);
        } catch (IOException e) {
            log.error("Unable to create directories for {}", path);
        }

        return root;
    }

    @Override
    public Optional<FileTransfer> transfer(StorageOperation operation) {
        Optional<FileTransfer> response = Optional.empty();
        OperationType type = operation.getType();
        if (contains(operation) && supported(type)) {
            if (type == OperationType.RSYNC) {
                Path rsyncPath = Paths.get(posix.getPath()).resolve(operation.getRoot());
                response = Optional.of(new RSyncTransfer(operation.getLink(), rsyncPath));
            } else {
                log.warn("Unable to support transfer of type {}", type);
            }
        }
        return response;
    }

    @Override
    public Optional<HashCode> hash(StorageOperation operation, Path file) {
        Optional<HashCode> response = Optional.empty();
        Path root = Paths.get(posix.getPath());
        Path resolved = root.resolve(operation.getRoot()).resolve(file);
        log.debug("[{}] Resolved file for hashing: {}", operation.getIdentifier(), resolved);
        if (resolved.toFile().exists()) {
            try {
                response = Optional.of(Files.asByteSource(resolved.toFile()).hash(Hashing.sha256()));
            } catch (IOException e) {
                log.error("[{}] Error hashing file", e);
            }
        }
        return response;
    }

    @Override
    public Optional<ByteSource> stream(StorageOperation operation, Path file) {
        // I'm not sure if this is really how we'll want to do this moving forward, but it's
        // what we have for now
        Path resolved = Paths.get(posix.getPath())
                .resolve(operation.getRoot())
                .resolve(file);
        return Optional.of(Files.asByteSource(resolved.toFile()));
    }

    @Override
    public GsonCollection.Builder fillAceStorage(StorageOperation operation, GsonCollection.Builder builder) {
        builder.storage("local");
        Path root;
        if (posix.getAce() != null) {
            root = Paths.get(posix.getAce());
        } else {
            root = Paths.get(posix.getPath());
        }

        builder.directory(root.resolve(operation.getRoot()).toString());

        return builder;
    }

    @Override
    public void free(StorageOperation operation) {
        pendingOperations.remove(operation);

        // update usable?? refresh??
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PosixBucket that = (PosixBucket) o;

        return posix != null ? posix.equals(that.posix) : that.posix == null;
    }

    @Override
    public int hashCode() {
        return posix != null ? posix.hashCode() : 0;
    }
}
