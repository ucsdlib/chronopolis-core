package org.chronopolis.ingest.models;

/**
 *
 * Created by shake on 4/25/17.
 */
public class FulfillmentRequest {

    private Long repair;
    private String node;

    public Long getRepair() {
        return repair;
    }

    public FulfillmentRequest setRepair(Long repair) {
        this.repair = repair;
        return this;
    }

    public String getNode() {
        return node;
    }

    public FulfillmentRequest setNode(String node) {
        this.node = node;
        return this;
    }
}
