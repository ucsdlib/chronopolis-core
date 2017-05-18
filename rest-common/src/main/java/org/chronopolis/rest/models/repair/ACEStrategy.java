package org.chronopolis.rest.models.repair;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.collect.ComparisonChain;
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
    public Strategy createEntity(org.chronopolis.rest.entities.Repair repair) {
        Ace strategy = new Ace();
        strategy.setRepair(repair);
        strategy.setApiKey(apiKey);
        strategy.setUrl(url);
        return strategy;
    }

    @Override
    public int compareTo(FulfillmentStrategy fulfillmentStrategy) {
        int compare;
        if (fulfillmentStrategy != null && fulfillmentStrategy.getType() == FulfillmentType.ACE) {
            ACEStrategy strategy = (ACEStrategy) fulfillmentStrategy;
            compare = ComparisonChain.start()
                    .compare(url, strategy.url)
                    .compare(apiKey, strategy.apiKey)
                    .result();
        } else {
            compare = -1;
        }

        return compare;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ACEStrategy that = (ACEStrategy) o;

        if (apiKey != null ? !apiKey.equals(that.apiKey) : that.apiKey != null) return false;
        return url != null ? url.equals(that.url) : that.url == null;
    }

    @Override
    public int hashCode() {
        int result = apiKey != null ? apiKey.hashCode() : 0;
        result = 31 * result + (url != null ? url.hashCode() : 0);
        return result;
    }
}
