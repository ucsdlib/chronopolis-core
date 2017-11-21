package org.chronopolis.rest.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ComparisonChain;
import org.chronopolis.rest.entities.storage.StagingStorage;
import org.chronopolis.rest.listener.BagUpdateListener;
import org.chronopolis.rest.models.BagStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import static org.chronopolis.rest.entities.BagDistribution.BagDistributionStatus;

/**
 * Representation of a bag in chronopolis
 *
 * TODO: Flesh out status and how to reflect that in chronopolis
 *
 * Created by shake on 11/5/14.
 */
@Entity
@EntityListeners(BagUpdateListener.class)
public class Bag extends UpdatableEntity implements Comparable<Bag> {

    @Transient
    private final Logger log = LoggerFactory.getLogger(Bag.class);

    @Transient
    private final int DEFAULT_REPLICATIONS = 3;

    private String name;
    private String creator;
    private String depositor;

    private long size;
    private long totalFiles;
    private int requiredReplications;

    @Enumerated(EnumType.STRING)
    private BagStatus status;

    // Might want to lazy fetch this if possible
    @OneToMany(mappedBy = "bag", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BagDistribution> distributions = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "bag_storage",
            joinColumns = @JoinColumn(name = "bag_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "staging_id", referencedColumnName = "id"))
    private Set<StagingStorage> bagStorage;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "token_storage",
            joinColumns = @JoinColumn(name = "bag_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "staging_id", referencedColumnName = "id"))
    private Set<StagingStorage> tokenStorage;

    protected Bag() { // JPA
    }

    public Bag(String name, String depositor) {
        this.name = name;
        this.depositor = depositor;
        this.status = BagStatus.DEPOSITED;
        this.requiredReplications = DEFAULT_REPLICATIONS;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
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

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public BagStatus getStatus() {
        return status;
    }

    public void setStatus(final BagStatus status) {
        this.status = status;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Bag bag = (Bag) o;

        if (!id.equals(bag.id)) return false;
        if (!depositor.equals(bag.depositor)) return false;
        return name.equals(bag.name);

    }

    @Override
    public int hashCode() {
        // Objects.hash(id, name, depositor);
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + depositor.hashCode();
        return result;
    }

    @Override
    public int compareTo(final Bag bag) {
        return ComparisonChain.start()
                .compare(id, bag.id)
                .compare(depositor, bag.depositor)
                .compare(name, bag.name)
                .result();

    }

    @Override
    public String toString() {
        return depositor + "::" + name;
    }

    public int getRequiredReplications() {
        return requiredReplications;
    }

    @JsonIgnore
    public Set<BagDistribution> getDistributions() {
        return distributions;
    }

    /**
     * Helper for adding a BagDistribution object to a Bag
     *
     * @param node The node who will receive the bag
     * @param status The initial status to use
     */
    public void addDistribution(Node node, BagDistributionStatus status) {
        BagDistribution distribution = new BagDistribution();
        distribution.setBag(this);
        distribution.setNode(node);
        distribution.setStatus(status);
        distributions.add(distribution);
    }

    public Set<String> getReplicatingNodes() {
        Set<String> replicatingNodes = new HashSet<>();
        for (BagDistribution distribution : distributions) {
            if (distribution.getStatus() == BagDistributionStatus.REPLICATE) {
                replicatingNodes.add(distribution.getNode().getUsername());
            }
        }
        return replicatingNodes;
    }

    public void addDistribution(BagDistribution dist) {
        distributions.add(dist);
    }

    public Bag setRequiredReplications(int requiredReplications) {
        this.requiredReplications = requiredReplications;
        return this;
    }

    public Set<StagingStorage> getBagStorage() {
        return bagStorage;
    }

    public Bag setBagStorage(StagingStorage storage) {
        if (bagStorage == null) {
            bagStorage = new HashSet<>();
        }
        bagStorage.add(storage);
        return this;
    }

    public Set<StagingStorage> getTokenStorage() {
        return tokenStorage;
    }

    public Bag setTokenStorage(StagingStorage storage) {
        if (tokenStorage == null) {
            tokenStorage = new HashSet<>();
        }
        this.tokenStorage.add(storage);
        return this;
    }

    public long getSize() {
        return size;
    }

    public Bag setSize(long size) {
        this.size = size;
        return this;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public Bag setTotalFiles(long totalFiles) {
        this.totalFiles = totalFiles;
        return this;
    }
}
