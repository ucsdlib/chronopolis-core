package org.chronopolis.ingest.models;

import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * Data required when creating a storage region
 *
 * Created by shake on 7/11/17.
 */
public class RegionCreate {

    @NotNull
    private String node;

    @NotNull
    @Min(1L)
    private Long capacity;

    @NotNull
    private DataType dataType;

    @NotNull
    private StorageType storageType;

    // Subtype using the ReplicationCreate? (explore after)
    private String replicationUser;

    @NotNull
    @NotEmpty
    private String replicationPath;

    @NotNull
    @NotEmpty
    private String replicationServer;

    public Long getCapacity() {
        return capacity;
    }

    public RegionCreate setCapacity(Long capacity) {
        this.capacity = capacity;
        return this;
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public RegionCreate setStorageType(StorageType storageType) {
        this.storageType = storageType;
        return this;
    }

    public String getReplicationUser() {
        return replicationUser;
    }

    public RegionCreate setReplicationUser(String replicationUser) {
        this.replicationUser = replicationUser;
        return this;
    }

    public String getReplicationPath() {
        return replicationPath;
    }

    public RegionCreate setReplicationPath(String replicationPath) {
        this.replicationPath = replicationPath;
        return this;
    }

    public String getReplicationServer() {
        return replicationServer;
    }

    public RegionCreate setReplicationServer(String replicationServer) {
        this.replicationServer = replicationServer;
        return this;
    }

    public String getNode() {
        return node;
    }

    public RegionCreate setNode(String node) {
        this.node = node;
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    public RegionCreate setDataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }
}
