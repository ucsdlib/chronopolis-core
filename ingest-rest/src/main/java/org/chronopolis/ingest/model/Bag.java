package org.chronopolis.ingest.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Representation of a bag in chronopolis
 *
 * Created by shake on 11/5/14.
 */
@Entity
public class Bag {

    @Id
    @GeneratedValue
    private Long id;

    private String name;
    private String depositor;
    private String location;
    private String tokenLocation;
    private String tokenDigest;
    private String tagManifestDigest;
    private String protocol;
    private String fixityAlgorithm;
    private long size;

    public Bag() {
    }

    public Bag(String name, String depositor) {
        this.name = name;
        this.depositor = depositor;
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

    public String getTokenDigest() {
        return tokenDigest;
    }

    public void setTokenDigest(final String tokenDigest) {
        this.tokenDigest = tokenDigest;
    }

    public String getTagManifestDigest() {
        return tagManifestDigest;
    }

    public void setTagManifestDigest(final String tagManifestDigest) {
        this.tagManifestDigest = tagManifestDigest;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
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
}
