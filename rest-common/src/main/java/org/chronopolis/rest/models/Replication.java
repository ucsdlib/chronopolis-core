package org.chronopolis.rest.models;


import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
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

    @ManyToOne
    @JoinColumn(name="ID")
    private Bag bag;

    private String bagLink;
    private String tokenLink;
    // TODO: enum type
    private String protocol;

    private String receivedTagFixity;
    private String receivedTokenFixity;

    // JPA...
    protected Replication() {
    }

    public Replication(final Node node,
                       final Bag bag) {
        this.status = ReplicationStatus.PENDING;
        this.node = node;
        this.bag = bag;
        // this.bagID = bagID;
        this.bagLink = "";
        this.tokenLink = "";
        this.protocol = "rsync";
    }

    public Replication(final Node node,
                       final Bag bag,
                       final String bagLink,
                       final String tokenLink) {
        this.status = ReplicationStatus.PENDING;
        this.node = node;
        this.bag = bag;
        this.bagLink = bagLink;
        this.tokenLink = tokenLink;
    }

    public Long getReplicationID() {
        return replicationID;
    }

    public Node getNode() {
        return node;
    }

    /*
    public Long getBagID() {
        return bagID;
    }
    */

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

    public Bag getBag() {
        return bag;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String resourceID() {
        return "replication/" + replicationID;
    }

}
