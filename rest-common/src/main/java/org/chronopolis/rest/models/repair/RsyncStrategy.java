package org.chronopolis.rest.models.repair;

/**
 *
 * Created by shake on 11/10/16.
 */
public class RsyncStrategy extends FulfillmentStrategy {

    private String link;

    public RsyncStrategy() {
    }

    public String getLink() {
        return link;
    }

    public RsyncStrategy setLink(String link) {
        this.link = link;
        return this;
    }
}
