package org.chronopolis.rest.entities.storage;

import org.chronopolis.rest.entities.PersistableEntity;

import javax.persistence.Entity;
import javax.persistence.OneToOne;

/**
 * Configuration Properties for a StorageRegion in order to create Replications
 *
 * Created by shake on 7/10/17.
 */
@Entity
public class ReplicationConfig extends PersistableEntity {

    @OneToOne
    private StorageRegion region;

    private String path;
    private String server;
    private String username;

    public ReplicationConfig() {
        // jpyay
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
