package org.chronopolis.ingest.model;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Created by shake on 11/5/14.
 */
@Entity
public class Replication {

    @Id
    @GeneratedValue
    private Long replicationID;

    @JsonIgnore
    @ManyToOne
    private Node node;

    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    private Long bagID;
    private String bagLink;
    private String tokenLink;
    // TODO: enum type
    private String protocol;

    private String receivedTagFixity;
    private String receivedTokenFixity;

    // JPA...
    Replication() {
    }

    public Replication(final Node node,
                       final Long bagID) {
        this.status = ReplicationStatus.STARTED;
        this.node = node;
        this.bagID = bagID;
        this.bagLink = "";
        this.tokenLink = "";
        this.protocol = "rsync";
    }

    public Replication(final Node node,
                       final Long bagID,
                       final String receivedTagFixity,
                       final String receivedTokenFixity) {
        this.status = ReplicationStatus.STARTED;
        this.node = node;
        this.bagID = bagID;
        this.receivedTagFixity = receivedTagFixity;
        this.receivedTokenFixity = receivedTokenFixity;
    }

    public Long getReplicationID() {
        return replicationID;
    }

    public Node getNode() {
        return node;
    }

    public Long getBagID() {
        return bagID;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ReplicationStatus status) {
        this.status = status;
    }

    public String getBagLink() {
        return bagLink;
    }

    public String getTokenLink() {
        return tokenLink;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setReceivedTokenFixity(final String receivedTokenFixity) {
        this.receivedTokenFixity = receivedTokenFixity;
    }

    public void setReceivedTagFixity(final String receivedTagFixity) {
        this.receivedTagFixity = receivedTagFixity;
    }

    public String getReceivedTagFixity() {
        return receivedTagFixity;
    }

    public String getReceivedTokenFixity() {
        return receivedTokenFixity;
    }

}
