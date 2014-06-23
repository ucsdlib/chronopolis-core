package org.chronopolis.db.model;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Created by shake on 6/12/14.
 */
@Entity
public class ReplicationFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Basic(optional = false)
    @Column(name = "ID", unique = true, nullable = false)
    private long id;

    @Column
    private String depositor;

    @Column
    private String collection;

    @Column
    private String node;

    @Column
    private String correlationId;

    @Column
    ReplicationState currentState;

    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(final String collection) {
        this.collection = collection;
    }

    public String getNode() {
        return node;
    }

    public void setNode(final String node) {
        this.node = node;
    }

    public ReplicationState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(final ReplicationState currentState) {
        this.currentState = currentState;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

}
