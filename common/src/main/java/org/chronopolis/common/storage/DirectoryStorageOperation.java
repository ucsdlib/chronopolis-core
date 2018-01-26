package org.chronopolis.common.storage;

import java.nio.file.Path;

/**
 * StorageOperation for recursive operations on directories
 *
 * @author shake
 */
public class DirectoryStorageOperation extends StorageOperation {

    private final Path root;

    public DirectoryStorageOperation(Path root) {
        this.root = root;
    }

    @Override
    public Path getRoot() {
        return root;
    }
}
