package org.chronopolis.db.common.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by shake on 8/8/14.
 */
@Entity
public class RestoreRequest {

    @Id
    @Column
    String correlationId;

    @Column
    String directory;

    @Column
    String depositor;

    @Column(name = "collection_name")
    String collectionName;

    @Column(name = "return_key")
    String returnKey;


    public RestoreRequest() {
    }

    public RestoreRequest(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final String correlationId) {
        this.correlationId = correlationId;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(final String directory) {
        this.directory = directory;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(final String collectionName) {
        this.collectionName = collectionName;
    }

    public String getDepositor() {
        return depositor;
    }

    public void setDepositor(final String depositor) {
        this.depositor = depositor;
    }

    public String getReturnKey() {
        return returnKey;
    }

    public void setReturnKey(final String returnKey) {
        this.returnKey = returnKey;
    }
}
