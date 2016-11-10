package org.chronopolis.rest.models.repair;

import java.time.ZonedDateTime;
import java.util.List;

/**
 *
 * Created by shake on 11/10/16.
 */
public class Repair {

    Long id;
    Long fulfillment;
    ZonedDateTime createdAt;
    ZonedDateTime updatedAt;
    RepairStatus status;
    String requester;
    String depositor;
    String collection;
    List<String> files;


}
