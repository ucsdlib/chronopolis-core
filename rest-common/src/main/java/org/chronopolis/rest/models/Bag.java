package org.chronopolis.rest.models;

import org.chronopolis.rest.models.storage.StagingStorageModel;

import java.time.ZonedDateTime;
import java.util.Set;

/**
 * Representation of a bag which is seen from outside the ingest server
 *
 * Created by shake on 8/1/16.
 */
public class Bag {
    private Long id;
    private StagingStorageModel bagStorage;
    private StagingStorageModel tokenStorage;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private String name;
    private String creator;
    private String depositor;
    private BagStatus status;
    private int requiredReplications;
    private Set<String> replicatingNodes;

    public Bag() {
    }

    public Long getId() {
        return id;
    }

    public Bag setId(Long id) {
        this.id = id;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Bag setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Bag setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public String getName() {
        return name;
    }

    public Bag setName(String name) {
        this.name = name;
        return this;
    }

    public String getCreator() {
        return creator;
    }

    public Bag setCreator(String creator) {
        this.creator = creator;
        return this;
    }

    public String getDepositor() {
        return depositor;
    }

    public Bag setDepositor(String depositor) {
        this.depositor = depositor;
        return this;
    }

    public BagStatus getStatus() {
        return status;
    }

    public Bag setStatus(BagStatus status) {
        this.status = status;
        return this;
    }

    public int getRequiredReplications() {
        return requiredReplications;
    }

    public Bag setRequiredReplications(int requiredReplications) {
        this.requiredReplications = requiredReplications;
        return this;
    }

    public Set<String> getReplicatingNodes() {
        return replicatingNodes;
    }

    public Bag setReplicatingNodes(Set<String> replicatingNodes) {
        this.replicatingNodes = replicatingNodes;
        return this;
    }

    public StagingStorageModel getBagStorage() {
        return bagStorage;
    }

    public Bag setBagStorage(StagingStorageModel bagStorage) {
        this.bagStorage = bagStorage;
        return this;
    }

    public StagingStorageModel getTokenStorage() {
        return tokenStorage;
    }

    public Bag setTokenStorage(StagingStorageModel tokenStorage) {
        this.tokenStorage = tokenStorage;
        return this;
    }
}
