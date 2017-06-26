package org.chronopolis.ingest.models;

/**
 *
 * Created by shake on 4/25/17.
 */
public class FulfillmentRequest {

    private Long repair;
    private String from;

    public Long getRepair() {
        return repair;
    }

    public FulfillmentRequest setRepair(Long repair) {
        this.repair = repair;
        return this;
    }

    public String getFrom() {
        return from;
    }

    public FulfillmentRequest setFrom(String from) {
        this.from = from;
        return this;
    }
}
