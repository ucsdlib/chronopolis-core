package org.chronopolis.replicate.processor;

import org.chronopolis.amqp.ChronProducer;
import org.chronopolis.common.ace.AceService;
import org.chronopolis.common.ace.GsonCollection;
import org.chronopolis.db.common.RestoreRepository;
import org.chronopolis.db.common.model.RestoreRequest;
import org.chronopolis.messaging.Indicator;
import org.chronopolis.messaging.base.ChronMessage;
import org.chronopolis.messaging.base.ChronProcessor;
import org.chronopolis.messaging.collection.CollectionRestoreReplyMessage;
import org.chronopolis.messaging.collection.CollectionRestoreRequestMessage;
import org.chronopolis.messaging.factory.MessageFactory;

/**
 * Created by shake on 8/7/14.
 */
@Deprecated
public class CollectionRestoreRequestProcessor implements ChronProcessor {

    private final ChronProducer producer;
    private final MessageFactory messageFactory;
    private final AceService aceService;
    private final RestoreRepository restoreRepository;

    public CollectionRestoreRequestProcessor(ChronProducer producer,
                                             MessageFactory messageFactory,
                                             AceService aceService,
                                             RestoreRepository restoreRepository) {
        this.producer = producer;
        this.messageFactory = messageFactory;
        this.aceService = aceService;
        this.restoreRepository = restoreRepository;
    }

    @Override
    public void process(final ChronMessage chronMessage) {
        if (!(chronMessage instanceof CollectionRestoreRequestMessage)) {
            throw new RuntimeException("Invalid message for " + this.getClass().getName());
        }

        CollectionRestoreRequestMessage msg = (CollectionRestoreRequestMessage) chronMessage;
        Indicator att = Indicator.ACK;

        // Check to make sure we actually have the collection
        // TODO: May want to make sure the collection is valid
        // GsonCollection collection = aceService.getCollectionByName(msg.getCollection(), msg.getDepositor());
        GsonCollection collection = null;
        if (collection == null) {
            att = Indicator.NAK;
        } else {
            RestoreRequest restore = new RestoreRequest(msg.getCorrelationId());
            restore.setDepositor(collection.getGroup());
            restore.setCollectionName(collection.getName());
            restore.setDirectory(collection.getDirectory());
            restoreRepository.save(restore);
        }

        CollectionRestoreReplyMessage reply =
                messageFactory.collectionRestoreReplyMessage(att,
                        msg.getCorrelationId());

        producer.send(reply, msg.getReturnKey());
    }

}
