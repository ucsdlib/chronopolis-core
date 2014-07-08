package org.chronopolis.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * TODO: Let's rename this to something better
 * TODO: I'm not sure if we really want the tokensGenerated field, but we have it for now
 *
 * Created by shake on 4/9/14.
 */
@Entity(name = "collection")
public class CollectionIngest {

    @Id
    @Column(name = "CORRELATION_ID")
    private String correlationId;

    @Column(name = "TO_DPN")
    private Boolean toDpn;

    @Column
    private String name;

    @Column
    private String depositor;

    @Column
    private Boolean tokensGenerated;

    @Column
    private String tokenDigest;

    @Column
    private String tagDigest;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getToDpn() {
        return toDpn;
    }

    public void setToDpn(Boolean toDpn) {
        this.toDpn = toDpn;
    }

    public String getTagDigest() {
        return tagDigest;
    }

    public void setTagDigest(final String tagDigest) {
        this.tagDigest = tagDigest;
    }

    public String getTokenDigest() {
        return tokenDigest;
    }

    public void setTokenDigest(final String tokenDigest) {
        this.tokenDigest = tokenDigest;
    }

    public Boolean getTokensGenerated() {
        return tokensGenerated;
    }

    public void setTokensGenerated(final Boolean tokensGenerated) {
        this.tokensGenerated = tokensGenerated;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

}
