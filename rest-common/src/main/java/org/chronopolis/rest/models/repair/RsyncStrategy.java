package org.chronopolis.rest.models.repair;

import org.chronopolis.rest.entities.fulfillment.Strategy;

/**
 *
 * Created by shake on 11/10/16.
 */
public class RsyncStrategy extends FulfillmentStrategy {

    private String link;

    public RsyncStrategy() {
        super(FulfillmentType.NODE_TO_NODE);
    }

    public String getLink() {
        return link;
    }

    public RsyncStrategy setLink(String link) {
        this.link = link;
        return this;
    }

    @Override
    public Strategy createEntity(org.chronopolis.rest.entities.Fulfillment fulfillment) {
        throw new UnsupportedOperationException("No entity for rsync exists yet");
    }
}
