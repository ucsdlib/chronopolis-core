package org.chronopolis.common.storage;

import com.google.common.collect.ComparisonChain;

import java.nio.file.Path;

/**
 * Base Class to represent a basic type of operation regardless of the Storage Type
 *
 * @author shake
 */
public abstract class StorageOperation implements Comparable<StorageOperation> {

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
     * The link/uri to transfer content with
     */
    private String link;

    /**
     * Retrieve the root directory for the operation
     *
     * @return the root of the operation
     */
    public abstract Path getRoot();

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

    public String getLink() {
        return link;
    }

    public StorageOperation setLink(String link) {
        this.link = link;
        return this;
    }

    @Override
    public int compareTo(StorageOperation operation) {
        return ComparisonChain.start()
                .compare(this.size, operation.size)
                .compare(this.type, operation.type)
                .compare(this.identifier, operation.identifier)
                .result();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StorageOperation operation = (StorageOperation) o;

        if (size != null ? !size.equals(operation.size) : operation.size != null) return false;
        if (type != operation.type) return false;
        return identifier != null ? identifier.equals(operation.identifier) : operation.identifier == null;
    }

    @Override
    public int hashCode() {
        int result = size != null ? size.hashCode() : 0;
        result = 31 * result + (type != null ? type.hashCode() : 0);
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        return result;
    }
}
