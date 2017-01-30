package org.chronopolis.rest.models.repair;

/**
 * Data containing the credentials in order to fulfill a repair
 *
 * Created by shake on 1/27/17.
 */
public class FulfillmentData {

    FulfillmentType type;
    FulfillmentStrategy credentials;

    public FulfillmentType getType() {
        return type;
    }

    public FulfillmentData setType(FulfillmentType type) {
        this.type = type;
        return this;
    }

    public FulfillmentStrategy getCredentials() {
        return credentials;
    }

    public FulfillmentData setCredentials(FulfillmentStrategy credentials) {
        this.credentials = credentials;
        return this;
    }
}
