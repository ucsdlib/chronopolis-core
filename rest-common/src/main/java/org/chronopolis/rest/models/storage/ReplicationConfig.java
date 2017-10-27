package org.chronopolis.rest.models.storage;

/**
 *
 * Created by shake on 7/11/17.
 */
public class ReplicationConfig {

    private Long region;
    private String path;
    private String server;
    private String username;

    public Long getRegion() {
        return region;
    }

    public ReplicationConfig setRegion(Long region) {
        this.region = region;
        return this;
    }

    public String getPath() {
        return path;
    }

    public ReplicationConfig setPath(String path) {
        this.path = path;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public ReplicationConfig setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getServer() {
        return server;
    }

    public ReplicationConfig setServer(String server) {
        this.server = server;
        return this;
    }
}
