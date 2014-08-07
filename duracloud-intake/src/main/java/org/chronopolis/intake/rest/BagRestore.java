package org.chronopolis.intake.rest;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.amqp.RoutingKey;
import org.chronopolis.db.intake.StatusRepository;
import org.chronopolis.db.intake.model.Status;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by shake on 7/10/14.
 */
@RestController
@RequestMapping("/api/restore")
public class BagRestore {
    private static final Logger log = LoggerFactory.getLogger(BagRestore.class);

    @Autowired
    private ChronProducer producer;

    @Autowired
    private MessageFactory messageFactory;

    @Autowired
    private StatusRepository statusRepository;

    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public ResponseEntity restore(@PathVariable("id") String snapshotId) {
        // grab the depositor and collection name from the bagstatus
        // and forward that through to the ingest service
        Status status = statusRepository.findByBagId(snapshotId);
        ResponseEntity entity;

        if (status == null) {
            log.info("Bag {} not found", snapshotId);
            entity = new ResponseEntity(HttpStatus.NOT_FOUND);
        } else if (status.isReplicated()) {
            log.info("Bag {} found and is replicated", snapshotId);
            entity = new ResponseEntity(HttpStatus.OK);
            ChronMessage message = messageFactory.collectionRestoreRequestMessage(
                    status.getCollectionName(),
                    status.getDepositor()
            );

            producer.send(message, RoutingKey.INGEST_BROADCAST.asRoute());
        } else {
            log.info("Bag {} found and is not replicated", snapshotId);
            entity = new ResponseEntity(HttpStatus.BAD_REQUEST);
        }

        return entity;
    }

}
