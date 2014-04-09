package org.chronopolis.db.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Created by shake on 4/9/14.
 */
@Entity(name = "collection_ingest")
public class CollectionIngest {
    @Id
    @Column(name = "CORRELATION_ID")
    private String correlationId;

    @Column(name = "TO_DPN")
    private Boolean toDpn;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public Boolean getToDpn() {
        return toDpn;
    }

    public void setToDpn(Boolean toDpn) {
        this.toDpn = toDpn;
    }
}
