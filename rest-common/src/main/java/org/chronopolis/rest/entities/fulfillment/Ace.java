package org.chronopolis.rest.entities.fulfillment;

import org.chronopolis.rest.models.repair.ACEStrategy;
import org.chronopolis.rest.models.repair.FulfillmentStrategy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 *
 * Created by shake on 11/11/16.
 */
@Entity
@DiscriminatorValue(value = "ACE")
public class Ace extends Strategy {

    String apiKey;
    String url;

    public Ace() {
    }

    public String getApiKey() {
        return apiKey;
    }

    public Ace setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public Ace setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public FulfillmentStrategy createModel() {
        return new ACEStrategy()
                .setUrl(url)
                .setApiKey(apiKey);
    }
}
