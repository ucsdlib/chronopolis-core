package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import static org.chronopolis.rest.models.BagDistribution.BagDistributionStatus;

/**
 * Representation of a bag in chronopolis
 *
 * TODO: Flesh out status and how to reflect that in chronopolis
 *
 * Created by shake on 11/5/14.
 */
@Entity
public class Bag implements Comparable<Bag> {
    @Transient
    private final Logger log = LoggerFactory.getLogger(Bag.class);
    
    @Transient
    private final int DEFAULT_REPLICATIONS = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String depositor;

    // Both locations are relative
    // TODO: It would be better to have them be Paths
    private String location;
    private String tokenLocation;

    private String tokenDigest;
    private String tagManifestDigest;

    @Enumerated(EnumType.STRING)
    private BagStatus status;

    private String fixityAlgorithm;
    private long size;
    private long totalFiles;

    private int requiredReplications;

    @OneToMany(mappedBy = "bag", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<BagDistribution> distributions = new HashSet<>();

    protected Bag() { // JPA
    }

    public Bag(String name, String depositor) {
        this.name = name;
        this.depositor = depositor;
        this.status = BagStatus.STAGED;
        this.requiredReplications = DEFAULT_REPLICATIONS;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getTokenLocation() {
        return tokenLocation;
    }

    public void setTokenLocation(final String tokenLocation) {
        this.tokenLocation = tokenLocation;
    }

    @JsonIgnore
    public String getTokenDigest() {
        return tokenDigest;
    }

    public void setTokenDigest(final String tokenDigest) {
        this.tokenDigest = tokenDigest;
    }

    @JsonIgnore
    public String getTagManifestDigest() {
        return tagManifestDigest;
    }

    public void setTagManifestDigest(final String tagManifestDigest) {
        this.tagManifestDigest = tagManifestDigest;
    }

    public String getFixityAlgorithm() {
        return fixityAlgorithm;
    }

    public void setFixityAlgorithm(final String fixityAlgorithm) {
        this.fixityAlgorithm = fixityAlgorithm;
    }

    public long getSize() {
        return size;
    }

    public void setSize(final long size) {
        this.size = size;
    }

    public String resourceID() {
        return "bag/" + id;
    }

    public long getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(final long totalFiles) {
        this.totalFiles = totalFiles;
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

        if (size != bag.size) return false;
        if (totalFiles != bag.totalFiles) return false;
        if (!id.equals(bag.id)) return false;
        if (!depositor.equals(bag.depositor)) return false;
        if (!fixityAlgorithm.equals(bag.fixityAlgorithm)) return false;
        if (!location.equals(bag.location)) return false;
        if (!name.equals(bag.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + depositor.hashCode();
        result = 31 * result + location.hashCode();
        result = 31 * result + fixityAlgorithm.hashCode();
        result = 31 * result + (int) (size ^ (size >>> 32));
        result = 31 * result + (int) (totalFiles ^ (totalFiles >>> 32));
        return result;
    }

    @Override
    public int compareTo(final Bag bag) {
        if (this.equals(bag)) {
            return 0;
        } else if (size > bag.size) {
            return 1;
        } else {
            return -1;
        }
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
}
