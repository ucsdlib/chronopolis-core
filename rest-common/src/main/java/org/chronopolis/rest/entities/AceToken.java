package org.chronopolis.rest.entities;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import java.util.Date;

/**
 * Entity representing an ACE Token in our DB
 *
 * Created by shake on 2/4/15.
 */
@Entity
public class AceToken extends PersistableEntity {

    @JoinColumn(name = "bag")
    @ManyToOne(fetch = FetchType.LAZY)
    private Bag bag;

    private Date createDate;
    private String filename;
    private String proof;
    private String imsHost;
    private String imsService;
    private String algorithm;
    private Long round;

    protected AceToken() { // JPA
    }

    public AceToken(final Bag bag,
                    final Date createDate,
                    final String filename,
                    final String proof,
                    final String imsHost,
                    final String imsService,
                    final String algorithm,
                    final Long round) {
        this.bag = bag;
        this.createDate = createDate;
        this.filename = filename;
        this.proof = proof;
        this.imsHost = imsHost;
        this.imsService = imsService;
        this.algorithm = algorithm;
        this.round = round;
    }

    public Bag getBag() {
        return bag;
    }

    public void setBag(final Bag bag) {
        this.bag = bag;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(final Date createDate) {
        this.createDate = createDate;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String filename) {
        this.filename = filename;
    }

    public String getProof() {
        return proof;
    }

    public void setProof(final String proof) {
        this.proof = proof;
    }

    public String getImsService() {
        return imsService;
    }

    public void setImsService(final String imsService) {
        this.imsService = imsService;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
    }

    public Long getRound() {
        return round;
    }

    public void setRound(final Long round) {
        this.round = round;
    }

    public String getImsHost() {
        return imsHost;
    }

    public AceToken setImsHost(String imsHost) {
        this.imsHost = imsHost;
        return this;
    }
}
