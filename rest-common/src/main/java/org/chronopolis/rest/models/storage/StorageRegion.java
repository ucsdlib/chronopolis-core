package org.chronopolis.rest.models.storage;


/**
 * Model for our StorageRegion entity
 *
 * Created by shake on 7/11/17.
 */
public class StorageRegion {

    private Long id;
    private String node;
    private Long capacity;
    private StorageType type;
    private ReplicationConfig replicationConfig;

    public String getNode() {
        return node;
    }

    public StorageRegion setNode(String node) {
        this.node = node;
        return this;
    }

    public Long getCapacity() {
        return capacity;
    }

    public StorageRegion setCapacity(Long capacity) {
        this.capacity = capacity;
        return this;
    }

    public StorageType getType() {
        return type;
    }

    public StorageRegion setType(StorageType type) {
        this.type = type;
        return this;
    }

    public ReplicationConfig getReplicationConfig() {
        return replicationConfig;
    }

    public StorageRegion setReplicationConfig(ReplicationConfig replicationConfig) {
        this.replicationConfig = replicationConfig;
        return this;
    }

    public Long getId() {
        return id;
    }

    public StorageRegion setId(Long id) {
        this.id = id;
        return this;
    }
}
