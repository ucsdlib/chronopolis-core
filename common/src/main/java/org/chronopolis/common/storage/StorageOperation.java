package org.chronopolis.common.storage;

import java.nio.file.Path;

/**
 * Class to represent a basic type of operation regardless of the Storage Type
 *
 * @author shake
 */
public class StorageOperation {

    /**
     * The size in bytes the operation will need
     */
    private Long size;

    /**
     * The type of operation to perform
     */
    private OperationType type;

    /**
     * An identifier for the operation
     */
    private String identifier;

    /**
     * The path to put the operation under
     */
    private Path path;

    /**
     * The link/uri to transfer content with
     */
    private String link;

    public Long getSize() {
        return size;
    }

    public StorageOperation setSize(Long size) {
        this.size = size;
        return this;
    }

    public OperationType getType() {
        return type;
    }

    public StorageOperation setType(OperationType type) {
        this.type = type;
        return this;
    }

    public String getIdentifier() {
        return identifier;
    }

    public StorageOperation setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    public Path getPath() {
        return path;
    }

    public StorageOperation setPath(Path path) {
        this.path = path;
        return this;
    }

    public String getLink() {
        return link;
    }

    public StorageOperation setLink(String link) {
        this.link = link;
        return this;
    }
}
