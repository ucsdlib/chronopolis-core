package org.chronopolis.rest.entities.storage;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

/**
 * Configuration Properties for a StorageRegion in order to create Replications
 *
 * Created by shake on 7/10/17.
 */
@Entity
public class ReplicationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @OneToOne
    private StorageRegion region;

    private String path;
    private String server;
    private String username;

    public ReplicationConfig() {
        // jpyay
    }

    public long getId() {
        return id;
    }

    public ReplicationConfig setId(long id) {
        this.id = id;
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

    public StorageRegion getRegion() {
        return region;
    }

    public ReplicationConfig setRegion(StorageRegion region) {
        this.region = region;
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
