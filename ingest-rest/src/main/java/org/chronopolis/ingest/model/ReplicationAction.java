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
public class ReplicationAction {

    @Id
    @GeneratedValue
    private Long actionID;

    @JsonIgnore
    @ManyToOne
    private Node node;

    @Enumerated(EnumType.STRING)
    public ReplicationStatus status;

    public Long bagID;
    public String expectedTagFixity;
    public String expectedTokenFixity;
    public String receivedTagFixity;
    public String receivedTokenFixity;

    // JPA...
    ReplicationAction() {
    }

    public ReplicationAction(final Node node,
                             final Long bagID,
                             final String expectedTagFixity,
                             final String expectedTokenFixity) {
        this.status = ReplicationStatus.STARTED;
        this.node = node;
        this.bagID = bagID;
        this.expectedTagFixity = expectedTagFixity;
        this.expectedTokenFixity = expectedTokenFixity;
    }

    public ReplicationAction(final Node node,
                             final Long bagID,
                             final String expectedTagFixity,
                             final String expectedTokenFixity,
                             final String receivedTagFixity,
                             final String receivedTokenFixity) {
        this.status = ReplicationStatus.STARTED;
        this.node = node;
        this.bagID = bagID;
        this.expectedTagFixity = expectedTagFixity;
        this.expectedTokenFixity = expectedTokenFixity;
        this.receivedTagFixity = receivedTagFixity;
        this.receivedTokenFixity = receivedTokenFixity;
    }

    public Long getActionID() {
        return actionID;
    }

    public Node getNode() {
        return node;
    }

    public Long getBagID() {
        return bagID;
    }

    public String getExpectedTagFixity() {
        return expectedTagFixity;
    }

    public String getExpectedTokenFixity() {
        return expectedTokenFixity;
    }

    public String getReceivedTagFixity() {
        return receivedTagFixity;
    }

    public String getReceivedTokenFixity() {
        return receivedTokenFixity;
    }

    public ReplicationStatus getStatus() {
        return status;
    }
}
