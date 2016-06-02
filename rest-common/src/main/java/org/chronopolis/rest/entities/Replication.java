package org.chronopolis.rest.entities;


import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.chronopolis.rest.listener.ReplicationUpdateListener;
import org.chronopolis.rest.models.ReplicationStatus;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import java.util.Objects;

/**
 * Representation of a Replication request
 *
 * TODO: Phase out the JsonGetters in favor of a separate class for "displaying" replications
 *
 * Created by shake on 11/5/14.
 */
@Entity
@EntityListeners(ReplicationUpdateListener.class)
public class Replication extends UpdatableEntity {

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
            if (node == null) {
                nodeUser = "";
            } else {
                nodeUser = node.getUsername();
            }
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

    public void checkTransferred() {
        String storedTagDigest = bag.getTagManifestDigest();
        String storedTokenDigest = bag.getTokenDigest();

        if (Objects.equals(storedTagDigest, receivedTagFixity)
                && Objects.equals(storedTokenDigest, receivedTokenFixity)) {
            this.status = ReplicationStatus.TRANSFERRED;
        }

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
