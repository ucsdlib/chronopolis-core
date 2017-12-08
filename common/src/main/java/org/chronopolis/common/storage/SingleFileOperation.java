package org.chronopolis.common.storage;

import java.nio.file.Path;

/**
 * Operation on a Single File
 *
 * @author shake
 */
public class SingleFileOperation extends StorageOperation {

    private final Path root;
    private final Path file;

    public SingleFileOperation(Path file) {
        this.root = file.getParent();
        this.file = file.getFileName();
    }

    @Override
    public Path getRoot() {
        return root;
    }

    public Path getFile() {
        return file;
    }
}
