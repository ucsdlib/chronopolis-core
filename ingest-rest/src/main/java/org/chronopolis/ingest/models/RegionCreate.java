package org.chronopolis.ingest.models;

import org.chronopolis.rest.models.storage.StorageType;

/**
 * Data required when creating a storage region
 *
 * Created by shake on 7/11/17.
 */
public class RegionCreate {

    private String node;
    private Long capacity;
    private StorageType type;
    // Subtype this?
    private String replicationUser;
    private String replicationPath;
    private String replicationServer;

    public Long getCapacity() {
        return capacity;
    }

    public RegionCreate setCapacity(Long capacity) {
        this.capacity = capacity;
        return this;
    }

    public StorageType getType() {
        return type;
    }

    public RegionCreate setType(StorageType type) {
        this.type = type;
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
}
