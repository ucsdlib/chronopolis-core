package org.chronopolis.db.replication.model;

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


    public RestoreRequest() {
    }

    public RestoreRequest(final String directory, final String correlationId) {
        this.directory = directory;
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
}
