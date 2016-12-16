package org.chronopolis.rest.models;

import java.time.ZonedDateTime;

/**
 *
 * Created by shake on 12/16/16.
 */
public class Replication {

    private Long id;
    private ZonedDateTime createdAt;
    private ZonedDateTime updatedAt;
    private ReplicationStatus status;
    private String bagLink;
    private String tokenLink;
    private String protocol;
    private String receivedTagFixity;
    private String receivedTokenFixity;
    private String node;
    private Bag bag;

    public Long getId() {
        return id;
    }

    public Replication setId(Long id) {
        this.id = id;
        return this;
    }

    public ZonedDateTime getCreatedAt() {
        return createdAt;
    }

    public Replication setCreatedAt(ZonedDateTime createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public ZonedDateTime getUpdatedAt() {
        return updatedAt;
    }

    public Replication setUpdatedAt(ZonedDateTime updatedAt) {
        this.updatedAt = updatedAt;
        return this;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public Replication setStatus(ReplicationStatus status) {
        this.status = status;
        return this;
    }

    public String getBagLink() {
        return bagLink;
    }

    public Replication setBagLink(String bagLink) {
        this.bagLink = bagLink;
        return this;
    }

    public String getTokenLink() {
        return tokenLink;
    }

    public Replication setTokenLink(String tokenLink) {
        this.tokenLink = tokenLink;
        return this;
    }

    public String getProtocol() {
        return protocol;
    }

    public Replication setProtocol(String protocol) {
        this.protocol = protocol;
        return this;
    }

    public String getReceivedTagFixity() {
        return receivedTagFixity;
    }

    public Replication setReceivedTagFixity(String receivedTagFixity) {
        this.receivedTagFixity = receivedTagFixity;
        return this;
    }

    public String getReceivedTokenFixity() {
        return receivedTokenFixity;
    }

    public Replication setReceivedTokenFixity(String receivedTokenFixity) {
        this.receivedTokenFixity = receivedTokenFixity;
        return this;
    }

    public String getNode() {
        return node;
    }

    public Replication setNode(String node) {
        this.node = node;
        return this;
    }

    public Bag getBag() {
        return bag;
    }

    public Replication setBag(Bag bag) {
        this.bag = bag;
        return this;
    }
}
