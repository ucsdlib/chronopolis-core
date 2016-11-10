package org.chronopolis.rest.models.repair;

import java.time.ZonedDateTime;

/**
 * Fulfillment of a repair
 *
 * Created by shake on 11/10/16.
 */
public class Fulfillment {

    Long id;
    Long repair;
    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;
    String to;
    String from;
    FulfillmentType type;
    FulfillmentStatus status;
    FulfillmentCredentials credentials;

}
