package org.chronopolis.db.intake.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by shake on 8/1/14.
 */
@Entity
public class Status {

    @Id
    @Column
    private String bagId;

    @Column(name = "depositor", nullable = false)
    private String depositor;

    @Column(name = "collection_name", nullable = false)
    private String collectionName;

    public Status() {
    }

    public Status(final String bagId,
                  final String depositor,
                  final String collectionName) {
        this.bagId = bagId;
        this.depositor = depositor;
        this.collectionName = collectionName;
    }

    public String getBagId() {
        return bagId;
    }

    public void setBagId(final String bagId) {
        this.bagId = bagId;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(final String collectionName) {
        this.collectionName = collectionName;
    }

    // TODO: Check based on status of all 3 nodes (saved from amqp responses)
    public boolean isReplicated() {
        return true;
    }

}
