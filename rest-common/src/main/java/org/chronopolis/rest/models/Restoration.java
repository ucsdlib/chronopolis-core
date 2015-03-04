package org.chronopolis.rest.models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Created by shake on 12/8/14.
 */
@Entity
public class Restoration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restorationId;

    @JsonIgnore
    @ManyToOne
    private Node node;

    @Enumerated(EnumType.STRING)
    private ReplicationStatus status;

    private String depositor;
    private String name;

    private String link;
    private String protocol;

    protected Restoration() { // JPA
    }

    public Restoration(final String depositor, final String name, final String link) {
        this.depositor = depositor;
        this.name = name;
        this.link = link;
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

    public String getLink() {
        return link;
    }

    public void setLink(final String link) {
        this.link = link;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(final String protocol) {
        this.protocol = protocol;
    }

    public String resourceID() {
        return "restore/" + restorationId;
    }

}
