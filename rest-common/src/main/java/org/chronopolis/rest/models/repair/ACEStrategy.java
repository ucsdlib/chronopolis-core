package org.chronopolis.rest.models.repair;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.chronopolis.rest.entities.fulfillment.Ace;
import org.chronopolis.rest.entities.fulfillment.Strategy;

/**
 *
 * Created by shake on 11/10/16.
 */
@JsonTypeName("ACE")
public class ACEStrategy extends FulfillmentStrategy {

    private String apiKey;
    private String url;

    public ACEStrategy() {
        super(FulfillmentType.ACE);
    }

    public String getApiKey() {
        return apiKey;
    }

    public ACEStrategy setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getUrl() {
        return url;
    }

    public ACEStrategy setUrl(String url) {
        this.url = url;
        return this;
    }

    @Override
    public Strategy createEntity() {
        Ace strategy = new Ace();
        strategy.setApiKey(apiKey);
        strategy.setUrl(url);
        return strategy;
    }
}
