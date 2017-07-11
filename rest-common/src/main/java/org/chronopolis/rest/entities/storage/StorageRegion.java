package org.chronopolis.rest.entities.storage;

import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.UpdatableEntity;
import org.chronopolis.rest.models.storage.StorageType;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import java.util.Set;

/**
 * A "region" of storage for a node -- i.e. some local holdings that they have
 *
 * Created by shake on 7/10/17.
 */
@Entity
public class StorageRegion extends UpdatableEntity {

    @ManyToOne
    private Node node;

    @Enumerated(value = EnumType.STRING)
    private StorageType type;

    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private Set<Storage> storage;

    @OneToOne(mappedBy = "region")
    private ReplicationConfig replicationConfig;

    private Long capacity;


    public StorageRegion() {
        // here for JPA
    }

    public Node getNode() {
        return node;
    }

    public StorageRegion setNode(Node node) {
        this.node= node;
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

    public Set<Storage> getStorage() {
        return storage;
    }

    public StorageRegion setStorage(Set<Storage> storage) {
        this.storage = storage;
        return this;
    }
}
