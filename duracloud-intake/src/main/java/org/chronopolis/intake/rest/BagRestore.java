package org.chronopolis.intake.rest;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.messaging.factory.MessageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 7/10/14.
 */
@RestController
@RequestMapping("/api/restore")
public class BagRestore {

    @Autowired
    private ChronProducer producer;

    @Autowired
    private MessageFactory messageFactory;

    public ResponseEntity restore(@PathVariable("id") String snapshotId) {
        return new ResponseEntity(HttpStatus.OK);
    }

    /*
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity restore(@PathVariable("id") String snapshotId) {
        // grab the depositor and collection name from the bagstatus
        // and forward that through to the ingest service
        BagStatus status = statusAccessor.get(snapshotId);
        ResponseEntity entity;

        if (status == null) {
            entity = new ResponseEntity(HttpStatus.NOT_FOUND);
        } else if (status.isReplicated()) {
            entity = new ResponseEntity(HttpStatus.OK);
            ChronMessage message = messageFactory.collectionRestoreRequestMessage(
                    status.getCollectionName(),
                    status.getDepositor()
            );

            producer.send(message, RoutingKey.INGEST_BROADCAST.asRoute());
        } else {
            entity = new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        return entity;
    }
    */

}
