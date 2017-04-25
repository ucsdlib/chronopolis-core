package org.chronopolis.ingest.models;

/**
 * Simple request to get collection information
 *
 * Created by shake on 4/21/17.
 */
public class CollectionInfo {

    private Long id;
    private String depositor;
    private String collection;

    public CollectionInfo() {
    }

    public Long id() {
        return id;
    }

    public CollectionInfo setId(Long id) {
        this.id = id;
        return this;
    }

    public String getDepositor() {
        return depositor;
    }

    public CollectionInfo setDepositor(String depositor) {
        this.depositor = depositor;
        return this;
    }

    public String getCollection() {
        return collection;
    }

    public CollectionInfo setCollection(String collection) {
        this.collection = collection;
        return this;
    }
}
