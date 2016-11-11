package org.chronopolis.rest.models.repair;

/**
 *
 * Created by shake on 11/10/16.
 */
public class ACEStrategy extends FulfillmentStrategy {

    private String apiKey;
    private String url;

    public ACEStrategy() {
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
}
