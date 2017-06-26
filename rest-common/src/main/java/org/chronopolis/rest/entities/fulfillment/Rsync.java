package org.chronopolis.rest.entities.fulfillment;

import org.chronopolis.rest.models.repair.FulfillmentStrategy;
import org.chronopolis.rest.models.repair.RsyncStrategy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 * Created by shake on 2/7/17.
 */
@Entity
@DiscriminatorValue("RSYNC")
public class Rsync extends Strategy {

    private String link;

    public String getLink() {
        return link;
    }

    public Rsync() { // JPA or something
    }

    public Rsync setLink(String link) {
        this.link = link;
        return this;
    }

    @Override
    public FulfillmentStrategy createModel() {
        return new RsyncStrategy()
                .setLink(link);
    }

}
