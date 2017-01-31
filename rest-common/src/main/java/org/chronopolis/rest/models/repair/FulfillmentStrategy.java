package org.chronopolis.rest.models.repair;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.chronopolis.rest.entities.fulfillment.Strategy;

/**
 * Base class for our credentials which are sent
 *
 * Created by shake on 11/10/16.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ACEStrategy.class, name = "ACE"),
        @JsonSubTypes.Type(value = RsyncStrategy.class, name = "NODE_TO_NODE"),
})
public abstract class FulfillmentStrategy {

    @JsonIgnore // I'm not sure if this is the best way to do this, but I figure
                // it's the best way to hold the FulfillmentType for other use
    private final FulfillmentType type;

    protected FulfillmentStrategy(FulfillmentType type) {
        this.type = type;
    }

    /**
     * Create a Strategy Entity for the given strategy type
     *
     * @return the StrategyEntity
     */
    public abstract Strategy createEntity(org.chronopolis.rest.entities.Fulfillment fulfillment);

    /**
     * Get the FulfillmentType associated with the Strategy
     *
     * @return the type of strategy
     */
    public FulfillmentType getType() {
        return type;
    }
}
