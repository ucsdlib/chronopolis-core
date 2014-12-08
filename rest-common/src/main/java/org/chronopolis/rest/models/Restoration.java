package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Created by shake on 12/8/14.
 */
@Entity
public class Restoration {

    @Id
    @GeneratedValue
    private Long restorationId;

    @JsonIgnore
    @ManyToOne
    private Node node;

    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    private String depositor;
    private String name;

    private String bagLink;
    private String protocol;

    Restoration() { // JPA
    }

    public Restoration(final String depositor, final String name) {
        this.depositor = depositor;
        this.name = name;
        this.status = ReplicationStatus.PENDING;
    }

    public Long getRestorationId() {
        return restorationId;
    }

    public void setRestorationId(final Long restorationId) {
        this.restorationId = restorationId;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(final Node node) {
        this.node = node;
    }

    public ReplicationStatus getStatus() {
        return status;
    }

    public void setStatus(final ReplicationStatus status) {
        this.status = status;
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

    public String getBagLink() {
        return bagLink;
    }

    public void setBagLink(final String bagLink) {
        this.bagLink = bagLink;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }
}
