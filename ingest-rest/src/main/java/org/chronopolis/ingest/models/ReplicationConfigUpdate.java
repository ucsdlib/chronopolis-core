package org.chronopolis.ingest.models;

/**
 * An update to a StorageRegion's replication configuration
 *
 */
public class ReplicationConfigUpdate {

    private String server;
    private String path;
    private String username;

    public String getServer() {
        return server;
    }

    public ReplicationConfigUpdate setServer(String server) {
        this.server = server;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ReplicationConfigUpdate setPath(String path) {
        this.path = path;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ReplicationConfigUpdate setUsername(String username) {
        this.username = username;
        return this;
    }
}
