package org.chronopolis.rest.models;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

/**
 * Representation of a Replication request
 *
 *
 * Created by shake on 11/5/14.
 */
@Entity
public class Replication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonIgnore
    private Node node;

    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    @ManyToOne
    @JsonIgnore
    private Bag bag;

    private String bagLink;
    private String tokenLink;
    // TODO: enum type
    private String protocol;

    private String receivedTagFixity;
    private String receivedTokenFixity;

    // For JSON (ignored because we use the JsonGetter/Setter below)
    @Transient
    @JsonIgnore
    private String nodeUser;

    @Transient
    @JsonIgnore
    private Long bagId;

    // JPA...
    protected Replication() {
    }

    public Replication(final Node node,
                       final Bag bag) {
        this.status = ReplicationStatus.PENDING;
        this.node = node;
        this.bag = bag;
        this.nodeUser = node.getUsername();
        this.bagId = bag.getId();
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
        this.nodeUser = node.getUsername();
        this.bagId = bag.getId();
        this.bagLink = bagLink;
        this.tokenLink = tokenLink;
    }

    public Long getId() {
        return id;
    }

    public Node getNode() {
        return node;
    }

    @JsonGetter("bagId")
    public Long getBagId() {
        // Because JPA/Hibernate sets fields through reflection,
        // this may need to be set here
        if (bagId == null) {
            bagId = bag.getId();
        }
        return bagId;
    }

    @JsonGetter("nodeUsername")
    public String getNodeUser() {
        if (nodeUser == null) {
            nodeUser = node.getUsername();
        }
        return nodeUser;
    }

    @JsonSetter("bagId")
    public void setBagId(Long id) {
        this.bagId = id;
    }

    @JsonSetter("nodeUsername")
    public void setNodeUser(String username) {
        this.nodeUser = username;
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

    public Bag getBag() {
        return bag;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public void setBag(Bag bag) {
        this.bag = bag;
    }
}
