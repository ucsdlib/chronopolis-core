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
    private final FileStore fileStore;
    private final Set<StorageOperation> pendingOperations;

    public PosixBucket(Posix posix, FileStore fileStore) {
        this.posix = posix;
        this.fileStore = fileStore;
        this.pendingOperations = new ConcurrentSkipListSet<>();
    }

    @Override
    public boolean allocate(StorageOperation operation) {
        boolean allocated = false;

        // do we want a lock when checking all of these? it would probably be useful but
        // I guess for now we can just use a single thread for allocation and what not
        if (contains(operation)) {
            allocated = true;
        } else if (writeable(operation) && supported(operation.getType())) {
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
        Path path = Paths.get(posix.getPath()).resolve(operation.getPath());
        return pendingOperations.contains(operation) || path.toFile().exists();
    }

    @Override
    public boolean writeable(StorageOperation operation) {
        boolean writeable = false;
        try {

            // this is pretty tricky as this is not entirely accurate
            // because pending operations could be going on while we compute
            // new totals. unfortunately with most transfers it's not really
            // possible to update the size as the transfer goes on because we
            // simply don't have access to that type of information. this means
            // we can either track the usable space on init and update it as
            // operations are added/removed or we can... do this. I don't think
            // tracking would be too much extra work, but it's additional overhead
            // which can be error prone.
            long total = fileStore.getTotalSpace();
            long usable = fileStore.getUsableSpace();
            for (StorageOperation op : pendingOperations) {
                usable += op.getSize();
            }

            long after = usable + operation.getSize();
            if (after < total && after/total < 0.9) {
                writeable = true;
            }
        } catch (IOException e) {
            log.error("[{}] Unable to read filestore!", operation.getIdentifier(), e);
        }
        return writeable;
    }

    @Override
    public Optional<FileTransfer> transfer(StorageOperation operation) {
        Optional<FileTransfer> response = Optional.empty();
        OperationType type = operation.getType();
        if (contains(operation) && supported(type)) {
            if (type == OperationType.RSYNC) {
                Path rsyncPath = Paths.get(posix.getPath()).resolve(operation.getPath());
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
        Path resolved = root.resolve(operation.getPath()).resolve(file);
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
        return Optional.empty();
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

        // this is missing the actual name of the bag
        // posix.getPath :: op.getPath
        // maps to
        // /root/operation/
        // instead of
        // /root/operation/{subfolder}
        builder.directory(root.resolve(operation.getPath()).toString());

        return builder;
    }

    @Override
    public void free(StorageOperation operation) {
        pendingOperations.remove(operation);
    }

    @Override
    public void refresh() {
        // noop
    }
}
