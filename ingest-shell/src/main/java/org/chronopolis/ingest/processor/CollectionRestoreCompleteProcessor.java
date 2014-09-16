package org.chronopolis.ingest.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreCompleteMessage;
import org.chronopolis.messaging.factory.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by shake on 8/8/14.
 */
public class CollectionRestoreCompleteProcessor implements ChronProcessor {
    private final Logger log = LoggerFactory.getLogger(CollectionRestoreCompleteProcessor.class);

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final RestoreRepository restoreRepository;

    public CollectionRestoreCompleteProcessor(ChronProducer producer,
                                              MessageFactory messageFactory,
                                              RestoreRepository restoreRepository) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.restoreRepository = restoreRepository;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreCompleteMessage)) {
            throw new RuntimeException("Invalid message for "
                    + this.getClass().getName()
                    + ": "
                    + chronMessage.getClass().getName());
        }

        CollectionRestoreCompleteMessage msg =
                (CollectionRestoreCompleteMessage) chronMessage;

        RestoreRequest request = restoreRepository.findByCorrelationId(msg.getCorrelationId());
        if (msg.getMessageAtt().equalsIgnoreCase(Indicator.ACK.toString())) {
            // notify intake of completion
            log.info("Successful restore for {}::{}", request.getDepositor(), request.getCollectionName());
            String location = request.getDirectory();
            String returnKey = request.getReturnKey();

            ChronMessage reply = messageFactory.collectionRestoreCompleteMessage(Indicator.ACK,
                    location,
                    msg.getCorrelationId());

            producer.send(reply, returnKey);
            restoreRepository.delete(request);
        } else {
            // Send mail notifying failure
            log.info("Error restoring {}::{}", request.getDepositor(), request.getCollectionName());
        }
    }
}
