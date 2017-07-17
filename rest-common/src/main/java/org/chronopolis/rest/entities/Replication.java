package org.chronopolis.rest.entities;


import org.chronopolis.rest.entities.storage.Fixity;
import org.chronopolis.rest.listener.ReplicationUpdateListener;
import org.chronopolis.rest.models.ReplicationStatus;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import java.util.Set;

/**
 * Representation of a Replication request
 *
 * Created by shake on 11/5/14.
 */
@Entity
@EntityListeners(ReplicationUpdateListener.class)
public class Replication extends UpdatableEntity {

    @ManyToOne
    private Node node;

    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    @ManyToOne(cascade = CascadeType.MERGE)
    private Bag bag;

    private String bagLink;
    private String tokenLink;
    // TODO: enum type
    private String protocol;

    private String receivedTagFixity;
    private String receivedTokenFixity;

    // For JSON (ignored because we use the JsonGetter/Setter below)
    /*
    @Transient
    @JsonIgnore
    private String nodeUser;

    @Transient
    @JsonIgnore
    private Long bagId;
    */

    // JPA...
    protected Replication() {
    }

    public Replication(final Node node,
                       final Bag bag) {
        this.status = ReplicationStatus.PENDING;
        this.node = node;
        this.bag = bag;
        // this.nodeUser = node.getUsername();
        // this.bagId = bag.getId();
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
        // this.nodeUser = node.getUsername();
        // this.bagId = bag.getId();
        this.bagLink = bagLink;
        this.tokenLink = tokenLink;
    }

    public Node getNode() {
        return node;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ReplicationStatus status) {
        this.status = status;
    }

    public void checkTransferred() {
        Set<Fixity> bagFixities = bag.getBagStorage().getFixities();
        Set<Fixity> tokenFixities = bag.getTokenStorage().getFixities();

        // Contains in fixities set
        if (status.isOngoing() &&
                fixityEquals(bagFixities, receivedTagFixity) &&
                fixityEquals(tokenFixities, receivedTokenFixity)) {
            this.status = ReplicationStatus.TRANSFERRED;
        }
    }

    private boolean fixityEquals(Set<Fixity> fixities, String received) {
        return fixities.stream()
                .anyMatch(fixity -> fixity.getValue().equalsIgnoreCase(received));

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

    /**
     * Update the receivedTokenFixity of a replication if it has not already been set
     *
     * @param fixity the received fixity value
     */
    public void setReceivedTokenFixity(final String fixity) {
        if (receivedTokenFixity == null) {
            this.receivedTokenFixity = fixity;
        }
    }

    /**
     * Update the received tag fixity of a replication if it has not already been set
     *
     * @param fixity the received fixity value
     */
    public void setReceivedTagFixity(final String fixity) {
        if (receivedTagFixity == null) {
            this.receivedTagFixity = fixity;
        }
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
