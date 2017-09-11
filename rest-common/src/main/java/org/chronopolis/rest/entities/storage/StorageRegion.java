package org.chronopolis.rest.entities.storage;

import org.chronopolis.rest.entities.Node;
import org.chronopolis.rest.entities.UpdatableEntity;
import org.chronopolis.rest.models.storage.DataType;
import org.chronopolis.rest.models.storage.StorageType;

import javax.persistence.CascadeType;
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

    // We probably don't need this field;
    // there's nothing wrong with having bags and tokens in the same SR,
    // just needs to be configured properly in the client
    @Enumerated(value = EnumType.STRING)
    private DataType dataType;

    @Enumerated(value = EnumType.STRING)
    private StorageType storageType;

    @OneToMany(mappedBy = "region", fetch = FetchType.LAZY)
    private Set<StagingStorage> storage;

    // might be able to change the cascade type
    @OneToOne(mappedBy = "region", cascade = CascadeType.ALL)
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

    public StorageType getStorageType() {
        return storageType;
    }

    public StorageRegion setStorageType(StorageType storageType) {
        this.storageType = storageType;
        return this;
    }

    public ReplicationConfig getReplicationConfig() {
        return replicationConfig;
    }

    public StorageRegion setReplicationConfig(ReplicationConfig replicationConfig) {
        this.replicationConfig = replicationConfig;
        return this;
    }

    public Set<StagingStorage> getStorage() {
        return storage;
    }

    public StorageRegion setStorage(Set<StagingStorage> storage) {
        this.storage = storage;
        return this;
    }

    public DataType getDataType() {
        return dataType;
    }

    public StorageRegion setDataType(DataType dataType) {
        this.dataType = dataType;
        return this;
    }
}