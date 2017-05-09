package org.chronopolis.rest.models.repair;

import org.chronopolis.rest.entities.fulfillment.Rsync;
import org.chronopolis.rest.entities.fulfillment.Strategy;

import java.util.Objects;

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
    public Strategy createEntity(org.chronopolis.rest.entities.Repair repair) {
        return new Rsync()
                .setLink(link);
    }

    @Override
    public int compareTo(FulfillmentStrategy fulfillmentStrategy) {
        int compare;
        if (fulfillmentStrategy != null && fulfillmentStrategy.getType() == FulfillmentType.NODE_TO_NODE) {
            RsyncStrategy strategy = (RsyncStrategy) fulfillmentStrategy;
            compare = Objects.compare(link, strategy.link, String::compareTo);
        } else {
            compare = -1;
        }

        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RsyncStrategy that = (RsyncStrategy) o;

        return link != null ? link.equals(that.link) : that.link == null;
    }

    @Override
    public int hashCode() {
        return link != null ? link.hashCode() : 0;
    }
}
